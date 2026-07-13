# Sodium FPS Step Fix

Complemento no oficial para Sodium (NO modifica su código). Usa un Mixin para
interceptar en tiempo de ejecución la llamada donde Sodium define el slider
de "Framerate Limit" y cambia su paso de `10` a `1`, permitiendo fijar
valores como 144, 165, etc.

## Cómo funciona

Dentro de `SodiumConfigBuilder#buildGeneralPage` (clase privada de Sodium),
la opción se crea así:

```java
builder.createIntegerOption(...)
       .setRange(10, 260, 10)   // min, max, step
```

`SodiumConfigBuilderMixin` usa `@Redirect` para interceptar **todas** las
llamadas a `setRange(int,int,int)` dentro de `buildGeneralPage`, y solo
cambia el `step` a `1` cuando `min == 10 && max == 260` (el rango exacto y
único del límite de FPS). Cualquier otra opción (GUI scale, brillo, etc.)
pasa intacta. Esto es más robusto que contar "cuál llamada es" por
posición, ya que no depende del orden en que Sodium declare sus opciones.

Sin embargo, esto solo arregla el **slider de Sodium**. Minecraft vanilla
guarda el límite de FPS internamente como un "escalón" de 1 a 26 (no como
el valor real), multiplicando/dividiendo por 10 al leer/escribir:

```java
new OptionInstance.IntRange(1, 26).xmap(value -> value * 10, value -> value / 10, true)
```

Por eso, aunque Sodium deje elegir 144, al aplicar Minecraft hacía
`144 / 10 = 14` y al releerlo `14 * 10 = 140`. `OptionsMixin` soluciona
esto: dentro del constructor de `Options` hay 6 llamadas al mismo método
`xmap(IntFunction, ToIntFunction, boolean)` (una por cada opción que
reescala su rango interno). La de `framerateLimit` es la **segunda**
(`ordinal = 1`, confirmado directamente sobre el código fuente de tu
versión de Minecraft). El Mixin reemplaza solo esa llamada por un
`IntRange(10, 260)` real, sin reescalar; las otras 5 quedan intactas.

**Nota:** a diferencia del Mixin de Sodium (que identifica su objetivo por
valores únicos, sin depender de posición), este SÍ usa una posición
(`ordinal`) dentro del código de Mojang, porque no hay ningún valor único
para distinguir esa llamada de las otras 5 (todas comparten la misma
firma). Si una futura versión de Minecraft reordena estas opciones, el
`ordinal` podría desalinearse — instrucciones para volver a verificarlo
están en los comentarios de `OptionsMixin.java`.

## Antes de compilar

En `gradle.properties`, cambia `sodium_version` por la misma versión de
Sodium (slug de Modrinth) que ya usas en tu modpack.

## Compilar

```
./gradlew build
```

El jar queda en `build/libs/`. Colócalo en `mods/` junto a Sodium y Fabric API
(no reemplaza a Sodium, es un mod aparte).

## Si el slider sigue en pasos de 10 / algo más deja de renderizarse

Si `min == 10 && max == 260` (en Sodium) coincidiera por casualidad con
otra opción en una futura versión, revisa
`SodiumConfigBuilder#buildGeneralPage` y ajusta la condición del
`@Redirect` de `SodiumConfigBuilderMixin`.

Si el límite de FPS vuelve a redondearse a múltiplos de 10 después de
actualizar Minecraft, es probable que el `ordinal` de `OptionsMixin` ya
no apunte a la llamada correcta. Repite el proceso descrito en los
comentarios de `OptionsMixin.java` (extraer `Options.java` descompilado y
contar de nuevo las llamadas `.xmap(...)` de 3 argumentos) para encontrar
el nuevo `ordinal`.

## Setup

For setup instructions, please see the [Fabric Documentation page](https://docs.fabricmc.net/develop/getting-started/creating-a-project#setting-up) related to the IDE that you are using.

## License

This template is available under the CC0 license. Feel free to learn from it and incorporate it in your own projects.
