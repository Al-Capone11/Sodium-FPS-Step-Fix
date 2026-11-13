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
 * Por eso el slider solo ofrece 140/150 y no 144: en el espacio real el
 * paso es 10 (next(1) -> 2 -> 20), y al aplicar Minecraft hacia 144 / 10 = 14
 * (division entera) y al leerlo de vuelta 14 * 10 = 140.
 *
 * Dentro del constructor de Options hay 6 llamadas a este mismo metodo
 * xmap(IntFunction, ToIntFunction, boolean) (una por cada opcion que
 * necesita reescalar su rango interno). La de framerateLimit es la
 * SEGUNDA (ordinal = 1, contando desde 0).
 *
 * VERIFICADO EN 26.3 con javap -p -c sobre net.minecraft.client.Options
 * (jar: .gradle/loom-cache/minecraftMaven/net/minecraft/
 * minecraft-merged-cad0293ec3/26.3/...): en <init> la 2da llamada a
 * IntRange.xmap(IntFunction, ToIntFunction, boolean) es la del offset 196,
 * precedida del ldc "options.framerateLimit" (offset 164) y seguida del
 * putfield framerateLimit (offset 220). Las 6 llamadas siguen en el mismo
 * orden que en 26.2, asi que ordinal = 1 sigue apuntando a framerateLimit.
 *
 * Este Mixin reemplaza SOLO esa 2da llamada: en vez de aplicar la
 * transformacion *10 //10, devuelve un IntRange real de 10 a 260 sin
 * reescalar, para que el valor guardado sea el FPS real. Las otras 5
 * llamadas de xmap quedan intactas.
 *
 * NOTA IMPORTANTE (26.3): el arreglo del slider de Sodium son DOS piezas
 * complementarias: este Mixin (lado Minecraft) y SodiumRangeMixin (lado
 * Sodium). Sodium >= 0.9.3-beta.1 ya no llama a setRange(10, 260, 10) en su
 * buildGeneralPage: obtiene min/max/step del propio OptionInstance vanilla
 * via VanillaOption#getRangeFor (min = fromSliderValue(0) = 10,
 * max = fromSliderValue(1) = 260, step = next(min) - min), asi que con el
 * IntRange(10, 260) que devolvemos aqui ya obtiene paso 1. Para las versiones
 * anteriores (0.9.2 y 0.9.3-alpha.1), que si traen el (10, 260, 10) escrito a
 * mano, el paso lo corrige SodiumRangeMixin a nivel del record Range. El
 * antiguo SodiumConfigBuilderMixin se elimino porque su selector de metodo
 * (buildGeneralPage(ConfigBuilder)) desaparecio en 0.9.3-beta.1 y hacia fallar
 * el arranque con InvalidInjectionException.
 *
 * ADVERTENCIA: a diferencia de los otros Mixins, este usa una posicion
 * (ordinal) dentro del codigo de Mojang. Si una version futura de Minecraft
 * reordena estas opciones dentro de Options, el ordinal puede dejar de
 * apuntar a framerateLimit. Para reverificarlo: javap -p -c sobre
 * net.minecraft.client.Options y contar las llamadas a
 * IntRange.xmap(IntFunction, ToIntFunction, boolean) dentro de <init>.
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
        return new OptionInstance.IntRange(10, 260);
    }
}
