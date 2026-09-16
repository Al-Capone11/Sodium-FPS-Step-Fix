# Sodium FPS Step Fix — Backport 1.20.1 (Fabric)

Mod cliente (mixins) para Sodium que:
1. Cambia el step del slider "Framerate Limit" de Sodium de 10 a 1.
2. Elimina el reescalado interno *10//10 de vanilla para que el FPS exacto (ej. 144) se respete y persista.
3. Reemplaza el limitador de FPS vanilla por `PreciseFrameLimiter` (park+spin adaptativo).

Backport de 1.21.1 → 1.20.1. **Cada supuesto se verificó contra los jars reales (Sodium 0.5.13+mc1.20.1 de Modrinth y MC 1.20.1 Mojang-mapped via genSources/loom), no por suposición.**

## Supuestos 1.21.1 → realidad 1.20.1 (con evidencia)

| Supuesto de 1.21.1 | Resultado en 1.20.1 | Evidencia |
|---|---|---|
| Sodium define el slider de FPS en `SodiumConfigBuilder.buildGeneralPage` con `IntegerOptionBuilder.setRange(10,260,10)` | **FALSO** — esa API es de Sodium 0.6+. En 0.5.13 (la de 1.20.1) el slider se construye en `SodiumGameOptionPages.general()`, lambda pasado a `OptionImpl.Builder#setControl`: `new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit())` | `javap -c` de `SodiumGameOptionPages` del jar real: `lambda$general$14` = `bipush 10, sipush 260, bipush 10` |
| Vanilla reescala framerateLimit con `IntRange.xmap(i -> i*10, i -> i/10)`, ordinal 1 en `Options.<init>` | **CIERTO** — idéntico en 1.20.1. `Options.java` (1.20.1, línea 124): `new OptionInstance.IntRange(1, 26).xmap(i -> i * 10, integer -> integer / 10)`. A bytecode: las 5 llamadas `IntRange.xmap` en `<init>` son, en orden: entityDistanceScaling (offset 115), framerateLimit (offset 178), chatDelay (offset 930), notificationDisplayTime (offset 994), mouseWheelSensitivity (offset 1221) → **ordinal 1 verificado** | genSources 1.20.1 + `javap -c` de `Options.<init>` |
| `RenderSystem.limitDisplayFPS(I)V`, `Minecraft.runTick(Z)V`, `Options.enableVsync()` existen igual | **CIERTO** — `Minecraft.java:1052 runTick(boolean)` invoca `RenderSystem.limitDisplayFPS(k)` con guard `k < 260`; `Options.enableVsync()` existe (línea 869); `RenderSystem.limitDisplayFPS(int)` existe (línea 202) | `Minecraft.java`, `RenderSystem.java`, `Options.java` de genSources 1.20.1 |
| `ResourceLocation.fromNamespaceAndPath` existe; helper `id()` | **FALSO/SOBRA** — `id()` no se usa en ningún lugar del código (grep). El helper y su import se eliminaron | grep de `SodiumFPSStepFix.id(` = 0 usos |
| Access widener: `OptionInstance$SliderableValueSet`, `$ValueSet`, `$IntRangeBase` necesarias | **CIERTO — NECESARIAS** — probado empíricamente: sin las líneas del AW, `javac` falla con "SliderableValueSet is not public in OptionInstance; cannot be accessed from outside package". El javap del jar de loom mostraba public solo porque el AW ya estaba aplicado (verificación circular); en 1.20.1 siguen siendo package-private | build sin AW → error del compilador; build con AW → verde |
| Sodium 0.5.x podría ya guardar FPS en pasos de 1 | **FALSO** — `SliderControl$Button.getIntValue()` = `min + interval * round(thumb*range/interval)`, y las flechas ±step usan `interval`; con interval=10 solo valores múltiplos de 10 | `javap -c SliderControl$Button` |
| Loom 1.17-SNAPSHOT y loader 0.19.3 no soportan 1.20.1 | **FALSO** — ambos funcionaron sin cambios | `./gradlew :fabric:build` verde con loom 1.17.21, loader 0.19.3 |
| Java 21 requerido | MC 1.20.1 requiere **Java 17** | cambios en build.gradle, mixins.json, fabric.mod.json |

## Cambios realizados

### Mixins (sobrevivieron / cambiaron / desaparecieron)
- **`MinecraftMixin` — SIN CAMBIOS** (firmas idénticas en 1.20.1).
- **`OptionsMixin` — SOBREVIVIÓ con ordinal verificado** (la escala *10//10 existe igual en 1.20.1). TODO resuelto con evidencia de bytecode.
- **`SodiumConfigBuilderMixin` — DESAPARECIÓ**, reemplazado por **`SliderControlMixin`**: `@Redirect` del constructor `new SliderControl(option, min, max, interval, formatter)` dentro de `SodiumGameOptionPages` (`method = "*"`), matching por valores `min==10 && max==260 && interval==10` → `interval=1`. Evidencia de unicidad: los 10 `new SliderControl` del jar 0.5.13 tienen rangos 0-9, 0-4, 1-7, 0-500/25, 10-260/10, 0-0, 0-100, 5-32, 2-32, 0-9 — solo el de FPS es 10-260.
- **`SodiumFPSStepFix.id()`** — eliminado (sin usos; `ResourceLocation.fromNamespaceAndPath` no existe en 1.20.1).

### Versiones (gradle.properties)
- `minecraft_version=1.20.1`, `mod_version=1.0.1-1.20.1`
- `fabric_api_version=0.92.9+1.20.1` (última publicada para 1.20.1)
- `sodium_version_fabric=mc1.20.1-0.5.13-fabric` (última para 1.20.1, Modrinth API)
- `loader_version=0.19.3` (funcionó — Fabric Loader es agnóstico al juego)

### Otros
- `build.gradle`: `options.release 21→17`, `source/targetCompatibility 21→17`
- `mixins.json`: `compatibilityLevel JAVA_21→JAVA_17`, `SodiumConfigBuilderMixin→SliderControlMixin`
- `fabric.mod.json`: `minecraft ~1.21.1→~1.20.1`, `fabricloader >=0.19.3→>=0.15.0`, `java >=21→>=17`
- `.github/workflows/build.yml`: `java-version 25→17` (distribution microsoft→temurin)
- Access widener: se mantienen las 3 líneas (necesarias en 1.20.1, verificado por compilación)
- `settings.gradle`: sin cambios (ya solo incluye 'fabric' en esta branch)

## Build
```bash
./gradlew :fabric:build   # BUILD SUCCESSFUL
```
Jar: `fabric/build/libs/sodium-fps-step-fix-fabric-1.0.1-1.20.1.jar` (+ sources). Verificado con `unzip -l` y `javap`: fabric.mod.json expandido con version 1.0.1-1.20.1, mixins remapeados a intermediary (`method_1523(Z)V` = runTick, `class_7172$class_7174;method_42414` = IntRange.xmap), AW en intermediary, icon.png, LICENSE.

## Nota sobre el ordinal en OptionsMixin
Este mixin usa ordinal=1, la única excepción documentada al matching por valores (heredada del diseño original). Riesgo: si Mojang reordena las opciones en `Options.<init>`, el mixin rompe. En 1.20.1 está verificado contra bytecode. (Un approach por valores puro no es posible aquí porque el xmap se llama con lambdas; el identificador es min/max del `new IntRange(1,26)` — ver "issues pendientes" del reporte.)

## Estado
Backport completado, build verde, commit local en branch `1.20.1` (sin push).
