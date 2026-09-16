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
 *     new OptionInstance.IntRange(1, 26).xmap(value -> value * 10, value -> value / 10)
 *
 * Por eso, aunque el slider de Sodium ya permita elegir 144, al aplicar
 * Minecraft hacia 144 / 10 = 14 (division entera) y al leerlo de vuelta
 * 14 * 10 = 140.
 *
 * En Minecraft 1.20.1 el constructor de Options llama a
 * OptionInstance$IntRange.xmap(IntFunction, ToIntFunction) 5 veces.
 * La de framerateLimit es la SEGUNDA (ordinal = 1, contando desde 0),
 * VERIFICADO directamente sobre el bytecode real de 1.20.1
 * (minecraft-merged...1_20_1, constructor <init> de net.minecraft.client.Options):
 *
 *   offset  115: IntRange.xmap(...) -> entityDistanceScaling (2-20, /4.0)
 *   offset  178: IntRange.xmap(...) -> framerateLimit (1-26, *10 //10)  <- la que arreglamos
 *   offset  930: IntRange.xmap(...) -> chatDelay (0-60, /10.0)
 *   offset  994: IntRange.xmap(...) -> notificationDisplayTime (5-100, /10.0)
 *   offset 1221: IntRange.xmap(...) -> mouseWheelSensitivity (-200..100, log/unlog)
 *
 * Este Mixin reemplaza SOLO esa 2da llamada: en vez de aplicar la
 * transformacion *10 //10, devuelve un IntRange real de 10 a 260 sin
 * reescalar, para que el valor guardado sea el FPS real. Las demas
 * llamadas de xmap quedan intactas.
 *
 * ADVERTENCIA: a diferencia de nuestro otro Mixin (que identifica su
 * objetivo por valores unicos), este usa una posicion (ordinal) dentro
 * del codigo de Mojang. Si una futura version de Minecraft reordena
 * estas opciones dentro de Options, el ordinal puede dejar de apuntar
 * a framerateLimit. Repite el proceso de "javap -c Options | grep xmap"
 * y cuenta de nuevo las llamadas a ".xmap(" si esto llega a pasar.
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
