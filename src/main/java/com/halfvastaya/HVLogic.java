package com.halfvastaya;

import java.util.List;

import net.minecraft.ChatFormatting;
import net.minecraft.core.Holder;
import net.minecraft.core.particles.DustColorTransitionOptions;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetSubtitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitleTextPacket;
import net.minecraft.network.protocol.game.ClientboundSetTitlesAnimationPacket;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.PacketDistributor;
import org.joml.Vector3f;

/** Toda la logica del servidor: progreso por dano, Furia, atributos y el golpe en cono. */
public final class HVLogic {
    public static final String ORIGIN_TAG = "halfvastaya_origin";

    public static final double RANGE = 6.0;
    public static final double HALF_ANGLE_DEG = 32.0;
    public static final float STRIKE_DAMAGE = 50.0f;

    private static final ResourceKey<DamageType> FURIA =
            ResourceKey.create(Registries.DAMAGE_TYPE, HalfVastaya.id("furia"));

    private static final ResourceLocation ATK2 = HalfVastaya.id("attack_lvl2");
    private static final ResourceLocation ARM2 = HalfVastaya.id("armor_lvl2");
    private static final ResourceLocation ATK3 = HalfVastaya.id("attack_lvl3");
    private static final ResourceLocation ARM3 = HalfVastaya.id("armor_lvl3");

    private static final Vector3f PINK = new Vector3f(1.0f, 0.55f, 0.85f);
    private static final Vector3f CYAN = new Vector3f(0.49f, 0.98f, 1.0f);
    private static final Vector3f PURPLE = new Vector3f(0.6f, 0.3f, 1.0f);

    private HVLogic() {
    }

    public static HVData data(ServerPlayer p) {
        return p.getData(ModAttachments.DATA);
    }

    public static boolean isActive(ServerPlayer p, HVData d) {
        return d.forced || (p.tickCount - d.originSeen) < 100;
    }

    public static void sync(ServerPlayer p, HVData d) {
        PacketDistributor.sendToPlayer(p, new Net.SyncPayload(isActive(p, d), d.level, d.damage, d.fury));
    }

    // ------------------------------------------------------------------ ciclo de vida

    public static void onJoin(ServerPlayer p) {
        HVData d = data(p);
        if (d.lastActive) {
            d.originSeen = p.tickCount; // periodo de gracia hasta que el origen vuelva a "avisar"
        }
        d.wasReady = d.fury >= HVData.READY;
        applyAttributes(p, d, isActive(p, d));
        sync(p, d);
    }

    public static void tick(ServerPlayer p) {
        HVData d = data(p);
        if (p.getTags().contains(ORIGIN_TAG)) {
            p.removeTag(ORIGIN_TAG);
            d.originSeen = p.tickCount;
        }
        boolean active = isActive(p, d);
        if (active != d.lastActive) {
            d.lastActive = active;
            applyAttributes(p, d, active);
            sync(p, d);
        }
        if (!active) {
            return;
        }

        boolean changed = false;
        if (d.fury > 0) {
            d.idle++;
            if (d.idle >= 60) { // 3 s sin recibir golpes: la Furia se enfria
                d.decayCtr++;
                if (d.decayCtr >= 4) {
                    d.decayCtr = 0;
                    d.fury--;
                    changed = true;
                }
            }
        }
        if (updateReady(p, d)) {
            changed = true;
        }
        if (changed) {
            sync(p, d);
        }
    }

    /** Avisa cuando la barra pasa a estar llena. Devuelve true si cambio el estado. */
    private static boolean updateReady(ServerPlayer p, HVData d) {
        boolean ready = d.fury >= HVData.READY;
        if (ready == d.wasReady) {
            return false;
        }
        d.wasReady = ready;
        if (ready) {
            ServerLevel level = p.serverLevel();
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:entity.experience_orb.pickup", 1.0f, 0.6f);
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:block.amethyst_block.resonate", 1.0f, 1.6f);
            p.displayClientMessage(gold(Component.translatable("halfvastaya.msg.ready")), true);
            level.sendParticles(new DustParticleOptions(PINK, 1.4f), p.getX(), p.getY() + 1.2, p.getZ(),
                    40, 0.4, 0.6, 0.4, 0.0);
            level.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1.2, p.getZ(),
                    15, 0.4, 0.6, 0.4, 0.2);
        }
        return true;
    }

    // ------------------------------------------------------------------ progreso por dano

    public static void onDamaged(ServerPlayer p, DamageSource source, float amount) {
        if (!(source.getEntity() instanceof LivingEntity attacker) || attacker == p) {
            return; // cuentan las heridas de criaturas y de otros jugadores (no el autodano)
        }
        HVData d = data(p);
        if (!isActive(p, d) || amount <= 0.0f) {
            return;
        }
        int points = Math.max(1, Math.round(amount));
        d.damage += points;
        d.fury = Math.min(HVData.MAX_FURY, d.fury + points * 4);
        d.idle = 0;
        d.decayCtr = 0;

        checkLevel(p, d);
        updateReady(p, d);
        sync(p, d);
    }

    public static void checkLevel(ServerPlayer p, HVData d) {
        boolean up = false;
        if (d.level < 2 && d.damage >= HVData.LEVEL2_DAMAGE) {
            d.level = 2;
            levelUp(p, 2);
            up = true;
        }
        if (d.level < 3 && d.damage >= HVData.LEVEL3_DAMAGE) {
            d.level = 3;
            levelUp(p, 3);
            up = true;
        }
        if (up) {
            applyAttributes(p, d, isActive(p, d));
        }
    }

    private static void levelUp(ServerPlayer p, int level) {
        ServerLevel w = p.serverLevel();
        boolean three = level >= 3;
        sound(w, p.getX(), p.getY(), p.getZ(), "minecraft:entity.player.levelup", 1.0f, three ? 0.5f : 0.7f);
        sound(w, p.getX(), p.getY(), p.getZ(), three ? "minecraft:block.beacon.power_select" : "minecraft:block.beacon.activate",
                0.9f, three ? 1.2f : 1.4f);
        if (three) {
            sound(w, p.getX(), p.getY(), p.getZ(), "minecraft:entity.ender_dragon.growl", 0.5f, 1.4f);
        }
        p.connection.send(new ClientboundSetTitlesAnimationPacket(10, three ? 70 : 50, 20));
        p.connection.send(new ClientboundSetSubtitleTextPacket(
                Component.translatable(three ? "halfvastaya.msg.level3" : "halfvastaya.msg.level2")
                        .withStyle(ChatFormatting.AQUA)));
        p.connection.send(new ClientboundSetTitleTextPacket(
                Component.translatable(three ? "halfvastaya.title.level3" : "halfvastaya.title.level2")
                        .withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD)));
        w.sendParticles(new DustParticleOptions(PINK, three ? 2.0f : 1.6f), p.getX(), p.getY() + 1.0, p.getZ(),
                three ? 150 : 90, 0.7, 1.1, 0.7, 0.0);
        w.sendParticles(ParticleTypes.CHERRY_LEAVES, p.getX(), p.getY() + 1.0, p.getZ(),
                three ? 60 : 40, 0.7, 1.1, 0.7, 0.3);
        w.sendParticles(ParticleTypes.END_ROD, p.getX(), p.getY() + 1.0, p.getZ(),
                three ? 40 : 25, 0.5, 1.0, 0.5, 0.1);
    }

    public static void applyAttributes(ServerPlayer p, HVData d, boolean active) {
        setMod(p, Attributes.ATTACK_DAMAGE, ATK2, 2.5, active && d.level >= 2);
        setMod(p, Attributes.ARMOR, ARM2, 3.0, active && d.level >= 2);
        setMod(p, Attributes.ATTACK_DAMAGE, ATK3, 3.5, active && d.level >= 3);
        setMod(p, Attributes.ARMOR, ARM3, 4.0, active && d.level >= 3);
    }

    private static void setMod(ServerPlayer p, Holder<Attribute> attribute, ResourceLocation id, double amount, boolean on) {
        AttributeInstance inst = p.getAttribute(attribute);
        if (inst == null) {
            return;
        }
        if (on) {
            inst.addOrReplacePermanentModifier(new AttributeModifier(id, amount, AttributeModifier.Operation.ADD_VALUE));
        } else {
            inst.removeModifier(id);
        }
    }

    // ------------------------------------------------------------------ Golpe Verdadero (cono multi-hit)

    public static void tryFury(ServerPlayer p) {
        HVData d = data(p);
        if (!isActive(p, d) || p.tickCount - d.lastCast < 20) {
            return;
        }
        ServerLevel level = p.serverLevel();
        if (d.fury < HVData.READY) {
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:block.dispenser.fail", 1.0f, 0.6f);
            p.displayClientMessage(Component.translatable("halfvastaya.msg.not_ready").withStyle(ChatFormatting.AQUA), true);
            return;
        }
        d.lastCast = p.tickCount;
        d.fury = 0;
        d.idle = 0;
        d.decayCtr = 0;
        d.wasReady = false;

        int hits = castCone(level, p);

        if (hits > 0) {
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:block.anvil.land", 0.7f, 0.6f);
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:entity.generic.explode", 0.5f, 1.4f);
            p.displayClientMessage(gold(Component.translatable("halfvastaya.msg.hit", hits)), true);
        } else {
            sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:entity.player.attack.nodamage", 1.0f, 0.8f);
            p.displayClientMessage(Component.translatable("halfvastaya.msg.miss").withStyle(ChatFormatting.AQUA), true);
        }
        sync(p, d);
    }

    /** Golpea a TODO lo que este dentro del cono frente al jugador. Devuelve cuantos objetivos alcanzo. */
    private static int castCone(ServerLevel level, ServerPlayer p) {
        Vec3 eye = p.getEyePosition();
        Vec3 look = p.getLookAngle().normalize();
        double halfAngle = Math.toRadians(HALF_ANGLE_DEG);

        sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:entity.evoker.cast_spell", 1.0f, 0.5f);
        sound(level, p.getX(), p.getY(), p.getZ(), "minecraft:block.beacon.activate", 0.8f, 1.8f);
        drawCone(level, eye, look);

        AABB box = new AABB(eye, eye.add(look.scale(RANGE))).inflate(RANGE * 0.65);
        List<LivingEntity> candidates = level.getEntitiesOfClass(LivingEntity.class, box, t ->
                t != p && t.isAlive() && !t.isSpectator() && !(t instanceof ArmorStand)
                        && !t.isAlliedTo(p)
                        && !(t instanceof TamableAnimal tame && tame.isOwnedBy(p)));

        DamageSource source = new DamageSource(
                level.registryAccess().registryOrThrow(Registries.DAMAGE_TYPE).getHolderOrThrow(FURIA), p);

        int hits = 0;
        for (LivingEntity t : candidates) {
            Vec3 center = t.getBoundingBox().getCenter();
            Vec3 to = center.subtract(eye);
            double dist = to.length();
            double radius = t.getBbWidth() * 0.5;
            if (dist - radius > RANGE) {
                continue;
            }
            if (dist > 0.001) {
                double angle = Math.acos(Math.max(-1.0, Math.min(1.0, to.normalize().dot(look))));
                double tolerance = Math.atan2(radius + 0.25, Math.max(dist, 0.5));
                if (angle - tolerance > halfAngle) {
                    continue;
                }
            }
            if (!p.hasLineOfSight(t)) {
                continue;
            }
            t.hurt(source, STRIKE_DAMAGE);
            hits++;

            level.sendParticles(new DustParticleOptions(PINK, 2.0f), center.x, center.y, center.z, 60, 0.4, 0.5, 0.4, 0.0);
            level.sendParticles(new DustColorTransitionOptions(PURPLE,
                    CYAN, 1.4f), center.x, center.y, center.z, 30, 0.3, 0.4, 0.3, 0.1);
            level.sendParticles(ParticleTypes.END_ROD, center.x, center.y, center.z, 15, 0.2, 0.3, 0.2, 0.2);
            level.sendParticles(ParticleTypes.DRAGON_BREATH, center.x, center.y, center.z, 20, 0.3, 0.4, 0.3, 0.4);
            sound(level, t.getX(), t.getY(), t.getZ(), "minecraft:entity.ravager.attack", 0.9f, 0.7f);
        }
        return hits;
    }

    /** Dibuja el cono con anillos rosados, celestes y morados que se ensanchan con la distancia. */
    private static void drawCone(ServerLevel level, Vec3 eye, Vec3 look) {
        Vec3 helper = Math.abs(look.y) > 0.99 ? new Vec3(1, 0, 0) : new Vec3(0, 1, 0);
        Vec3 right = look.cross(helper).normalize();
        Vec3 up = right.cross(look).normalize();
        ParticleOptions dustPink = new DustParticleOptions(PINK, 1.0f);
        ParticleOptions dustCyan = new DustParticleOptions(CYAN, 1.0f);
        ParticleOptions dustPurple = new DustParticleOptions(PURPLE, 1.0f);
        double tan = Math.tan(Math.toRadians(HALF_ANGLE_DEG));
        Vec3 origin = eye.add(0, -0.4, 0);

        for (int d = 1; d <= (int) RANGE; d++) {
            double r = d * tan;
            for (int a = 0; a < 360; a += 30) {
                double rad = Math.toRadians(a);
                Vec3 pt = origin.add(look.scale(d)).add(right.scale(Math.cos(rad) * r)).add(up.scale(Math.sin(rad) * r));
                ParticleOptions dust = (a / 30) % 3 == 0 ? dustPink : (a / 30) % 3 == 1 ? dustCyan : dustPurple;
                level.sendParticles(dust, pt.x, pt.y, pt.z, 1, 0, 0, 0, 0);
            }
            Vec3 c = origin.add(look.scale(d));
            level.sendParticles(ParticleTypes.END_ROD, c.x, c.y, c.z, 1, 0.1, 0.1, 0.1, 0.01);
        }
        Vec3 mid = origin.add(look.scale(3));
        level.sendParticles(ParticleTypes.CHERRY_LEAVES, mid.x, mid.y, mid.z, 25, 1.2, 0.8, 1.2, 0.2);
    }

    // ------------------------------------------------------------------ utilidades

    private static Component gold(Component c) {
        return ((net.minecraft.network.chat.MutableComponent) c).withStyle(ChatFormatting.LIGHT_PURPLE, ChatFormatting.BOLD);
    }

    public static void sound(ServerLevel level, double x, double y, double z, String id, float volume, float pitch) {
        SoundEvent ev = BuiltInRegistries.SOUND_EVENT.get(ResourceLocation.parse(id));
        if (ev != null) {
            level.playSound(null, x, y, z, ev, SoundSource.PLAYERS, volume, pitch);
        }
    }
}
