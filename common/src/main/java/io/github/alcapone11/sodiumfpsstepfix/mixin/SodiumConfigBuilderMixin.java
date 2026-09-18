package io.github.alcapone11.sodiumfpsstepfix.mixin;

import net.caffeinemc.mods.sodium.api.config.structure.IntegerOptionBuilder;
import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Intercepta la construccion de la pagina "General" de las opciones de video
 * de Sodium para cambiar el paso (step) del slider de "Framerate Limit".
 *
 * Sodium define esa opcion asi (metodo privado buildGeneralPage):
 *
 *     builder.createIntegerOption(...)
 *            ...
 *            .setRange(10, 260, 10)   <- min, max, step
 *            ...
 *
 * IMPORTANTE: en vez de contar "cual llamada a setRange es" (un ordinal
 * posicional, fragil ante cambios de orden entre versiones de Sodium),
 * este @Redirect intercepta TODAS las llamadas a setRange(int,int,int)
 * dentro de buildGeneralPage, y solo modifica el step cuando los valores
 * de min/max coinciden EXACTAMENTE con los del framerate limit (10, 260).
 * Cualquier otra opcion (GUI scale, brightness, etc.) pasa sin cambios.
 *
 * Esto es mucho mas robusto: no importa cuantas opciones tenga tu version
 * de Sodium ni en que orden esten, siempre se identifica la correcta por
 * su rango real en vez de por su posicion.
 */
@Mixin(SodiumConfigBuilder.class)
public class SodiumConfigBuilderMixin {

    @Redirect(
            method = "buildGeneralPage(Lnet/caffeinemc/mods/sodium/api/config/structure/ConfigBuilder;)Lnet/caffeinemc/mods/sodium/api/config/structure/OptionPageBuilder;",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;setRange(III)Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;"
            )
    )
    private IntegerOptionBuilder sodiumFpsStepFix$redirectSetRange(IntegerOptionBuilder builder, int min, int max, int step) {
        if (min == 10 && max == 260) {
            // Esta es la llamada del limite de FPS: forzamos paso = 1.
            return builder.setRange(min, max, 1);
        }
        // Cualquier otra opcion (GUI scale, brillo, etc.): sin cambios.
        return builder.setRange(min, max, step);
    }
}
