package io.github.alcapone11.sodiumfpsstepfix.mixin;

import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.util.function.IntFunction;
import java.util.function.ToIntFunction;

/**
 * Minecraft vanilla NO guarda el limite de FPS como el valor real (10-260),
 * sino como un "escalon" interno de 1 a 26 que luego se multiplica/divide
 * por 10, usando IntRangeBase#xmap(...):
 *
 *     new OptionInstance.IntRange(1, 26).xmap(value -> value * 10, value -> value / 10, true)
 *
 * Por eso, aunque Sodium ya permita elegir 144, al aplicar Minecraft hacia
 * 144 / 10 = 14 (division entera) y al leerlo de vuelta 14 * 10 = 140.
 *
 * Dentro del constructor de Options hay 6 llamadas a este mismo metodo
 * xmap(IntFunction, ToIntFunction, boolean) (una por cada opcion que
 * necesita reescalar su rango interno). La de framerateLimit es la
 * SEGUNDA (ordinal = 1, contando desde 0), confirmado directamente sobre
 * el codigo fuente de esta version de Minecraft (26.2):
 *
 *   ordinal 0 -> guiScale-like (linea 117, /4.0)
 *   ordinal 1 -> framerateLimit (linea 129, *10 / 10)  <- la que arreglamos
 *   ordinal 2..5 -> otras opciones (brillo, distancia niebla, etc.)
 *
 * Este Mixin reemplaza SOLO esa 2da llamada: en vez de aplicar la
 * transformacion *10 //10, devuelve un IntRange real de 10 a 260 sin
 * reescalar, para que el valor guardado sea el FPS real. Las otras 5
 * llamadas de xmap quedan intactas.
 *
 * ADVERTENCIA: a diferencia de nuestro otro Mixin (que identifica su
 * objetivo por valores unicos), este usa una posicion (ordinal) dentro
 * del codigo de Mojang. Si una futura version de Minecraft reordena estas
 * opciones dentro de Options, el ordinal puede dejar de apuntar a
 * framerateLimit. Repita el proceso de "jar xf ... Options.java" y
 * cuente de nuevo las llamadas a ".xmap(" con 3 argumentos si esto llega
 * a pasar.
 *
 * ORDINAL VERIFICADO PARA 1.21.1 (2026-09-15):
 * - `./gradlew :fabric:genSources` -> Options.java de MC 1.21.1 (mappings Mojang):
 *   linea 117:  new OptionInstance.IntRange(2, 20).xmap(i -> i / 4.0, ...)  -> entityDistanceScaling (ordinal 0)
 *   linea 129:  new OptionInstance.IntRange(1, 26).xmap(i -> i * 10, integer -> integer / 10) -> framerateLimit (ordinal 1)
 * - Bytecode del constructor <init> (javap -c sobre neoforge-21.1.51-merged.jar):
 *   offset 132:  xmap de IntRange(2,20)   (entityDistanceScaling)  -> ordinal 0
 *   offset 195:  xmap de IntRange(1,26), precedido por ldc "options.framerateLimit" -> ordinal 1
 *   despues siguen: chatDelay (IntRange(0,60), ordinal 2), notificationDisplayTime
 *   (IntRange(5,100), ordinal 3) y mouseWheelSensitivity (IntRange(-200,100), ordinal 4).
 * El campo framerateLimit sigue siendo la SEGUNDA llamada xmap(IntFunction, ToIntFunction)
 * dentro de <init> => ordinal = 1 es CORRECTO en 1.21.1.
 */
@Mixin(Options.class)
public class OptionsMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
            method = "<init>",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance$IntRange;xmap(Ljava/util/function/IntFunction;Ljava/util/function/ToIntFunction;)Lnet/minecraft/client/OptionInstance$SliderableValueSet;",
                    ordinal = 1
            )
    )
    private OptionInstance.SliderableValueSet sodiumFpsStepFix$fixFramerateLimitRange(
            OptionInstance.IntRange self,
            IntFunction to,
            ToIntFunction from
    ) {
        // Rango real 10-260, sin reescalar a "escalones" de 1-26.
        return new OptionInstance.IntRange(10, 260);
    }
}
