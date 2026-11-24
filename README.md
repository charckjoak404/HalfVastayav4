# Half-Vastaya (mod para NeoForge 1.21.1)

Origen inspirado en Sett Flor Espiritual (LoL), con paleta morada, rosada y celeste. Funciona con **Origins** (Apoli, tipos `apoli:`); ya no usa NeoOrigins.

- **Niveles por daño recibido** de criaturas y de otros jugadores: Nivel II a los 500 y Nivel III a los 1200 puntos de daño (Fuerza y Resistencia suben).
- **Barra de Furia** rosa-morada en pantalla, dibujada **por encima de la barra de armadura** (se adapta a varias filas de corazones).
- **Golpe Verdadero (tecla R)**: con la barra llena lanza un **cono de 6 bloques** con pétalos y destellos rosados, morados y celestes, que alcanza a **todos** los que estén dentro, 50 de daño verdadero que ignora la armadura.
- **Panel de progreso (tecla G)**, solo para jugadores con el origen Half-Vastaya: nivel, camino I-II-III, evolución, Furia, bonificaciones y controles actuales.
- Teclas configurables en *Opciones > Controles > Half-Vastaya*.

## Cómo conseguir el .jar

### Opción A: GitHub (sin instalar nada)
1. Crea un repositorio en GitHub y sube todo el contenido de esta carpeta (incluida `.github`).
2. En la pestaña **Actions** se ejecuta "Build mod" solo. Al terminar, descarga el artefacto `halfvastaya-jar`.

### Opción B: en tu PC
Necesitas JDK 21 y Gradle 8.10 o superior:

    gradle build

El archivo queda en `build/libs/halfvastaya-1.0.2.jar`.

## Instalación
1. Pon el .jar en la carpeta `mods` (junto a Origins).
2. **Quita los datapacks anteriores de Half-Vastaya** del mundo: el origen ya viene dentro del mod.
3. Entra al mundo y elige el origen **Half-Vastaya** en la pantalla de orígenes.

## Cómo se conecta con el origen
El origen (en `data/halfvastaya/`) usa poderes de **Origins/Apoli** (`apoli:action_over_time` y `apoli:execute_command`) y marca al jugador cada 2 segundos.
El mod detecta esa marca para activar niveles, Furia y habilidades. Sin Origins,
un operador puede activarlo a mano:

    /halfvastaya enable <jugadores>
    /halfvastaya disable <jugadores>
    /halfvastaya level <jugadores> <1-3>
    /halfvastaya fury <jugadores> <0-100>

## Ajustes rápidos (en `HVData.java` y `HVLogic.java`)
- Umbrales de nivel: `LEVEL2_DAMAGE`, `LEVEL3_DAMAGE`.
- Furia lista: `READY` (75) y ganancia por golpe (`points * 4` en `HVLogic.onDamaged`).
- Cono: `RANGE` (6 bloques) y `HALF_ANGLE_DEG` (32°), daño `STRIKE_DAMAGE` (50).
