package com.halfvastaya;

import java.util.Collection;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;

import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/** /halfvastaya enable|disable|level|fury  (solo operadores) */
public final class HVCommands {
    private HVCommands() {
    }

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(Commands.literal("halfvastaya")
                .requires(s -> s.hasPermission(2))
                .then(Commands.literal("enable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forced(c, true))))
                .then(Commands.literal("disable")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .executes(c -> forced(c, false))))
                .then(Commands.literal("level")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(1, 3))
                                        .executes(HVCommands::level))))
                .then(Commands.literal("fury")
                        .then(Commands.argument("targets", EntityArgument.players())
                                .then(Commands.argument("value", IntegerArgumentType.integer(0, HVData.MAX_FURY))
                                        .executes(HVCommands::fury)))));
    }

    private static int forced(CommandContext<CommandSourceStack> c, boolean on) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        for (ServerPlayer p : players) {
            HVData d = HVLogic.data(p);
            d.forced = on;
            if (on) {
                d.lastActive = false; // fuerza que el siguiente tick aplique atributos y sincronice
            }
        }
        return done(c, players.size());
    }

    private static int level(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int v = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) {
            HVData d = HVLogic.data(p);
            d.level = v;
            d.damage = v == 1 ? 0 : v == 2 ? HVData.LEVEL2_DAMAGE : HVData.LEVEL3_DAMAGE;
            HVLogic.applyAttributes(p, d, HVLogic.isActive(p, d));
            HVLogic.sync(p, d);
        }
        return done(c, players.size());
    }

    private static int fury(CommandContext<CommandSourceStack> c) throws CommandSyntaxException {
        Collection<ServerPlayer> players = EntityArgument.getPlayers(c, "targets");
        int v = IntegerArgumentType.getInteger(c, "value");
        for (ServerPlayer p : players) {
            HVData d = HVLogic.data(p);
            d.fury = v;
            d.idle = 0;
            HVLogic.sync(p, d);
        }
        return done(c, players.size());
    }

    private static int done(CommandContext<CommandSourceStack> c, int n) {
        c.getSource().sendSuccess(() -> Component.translatable("halfvastaya.cmd.ok", n), true);
        return n;
    }
}
