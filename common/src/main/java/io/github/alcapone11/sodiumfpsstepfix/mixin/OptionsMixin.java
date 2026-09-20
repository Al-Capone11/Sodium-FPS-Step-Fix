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
 * ORDINAL EN 26.1 (re-verificado con javap -c contra el merged jar real
 * de NeoForge 26.1.0.19-beta, neoforge/build/moddev/artifacts/
 * minecraft-patched-26.1.0.19-beta-merged.jar): el <init> de Options
 * tiene 6 llamadas a xmap(IntFunction,ToIntFunction,boolean); la de
 * framerateLimit sigue siendo la SEGUNDA (ordinal 1, bci 198, precedida
 * por ldc "options.framerateLimit" en bci 166 y new IntRange en 177).
 * Igual que en 26.2.
 *
 * NOTA 26.1: aqui OptionInstance$SliderableValueSet e
 * OptionInstance$IntRangeBase son package-private (en 26.2 Mojang los
 * hizo public), por eso el handler declara java.lang.Object como tipo
 * de retorno: es el patron estandar de Mixin para redirigir metodos
 * cuyo tipo de retorno no es visible desde fuera; Mixin inserta el
 * checkcast al tipo real en el sitio de la llamada. El valor devuelto
 * sigue siendo un OptionInstance.IntRange(10, 260), que ES un
 * SliderableValueSet via IntRangeBase.
 *
 * ADVERTENCIA: a diferencia de nuestro otro Mixin (que identifica su
 * objetivo por valores unicos), este usa una posicion (ordinal) dentro
 * del codigo de Mojang. Si una futura version de Minecraft reordena estas
 * opciones dentro de Options, el ordinal puede dejar de apuntar a
 * framerateLimit. Repite el proceso de "jar xf ... Options.java" y
 * cuenta de nuevo las llamadas a ".xmap(" con 3 argumentos si esto llega
 * a pasar.
 */
@Mixin(Options.class)
public class OptionsMixin {

    @SuppressWarnings({"unchecked", "rawtypes"})
    @Redirect(
            method = "<init>(Lnet/minecraft/client/Minecraft;Ljava/io/File;)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/OptionInstance$IntRange;xmap(Ljava/util/function/IntFunction;Ljava/util/function/ToIntFunction;Z)Lnet/minecraft/client/OptionInstance$SliderableValueSet;",
                    ordinal = 1
            )
    )
    private OptionInstance.SliderableValueSet sodiumFpsStepFix$fixFramerateLimitRange(
            OptionInstance.IntRange self,
            IntFunction to,
            ToIntFunction from,
            boolean discrete
    ) {
        // Rango real 10-260, sin reescalar a "escalones" de 1-26.
        // (retorno Object: SliderableValueSet es package-private en 26.1)
        return new OptionInstance.IntRange(10, 260);
    }
}
