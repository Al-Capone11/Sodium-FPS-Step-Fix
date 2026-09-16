package io.github.alcapone11.sodiumfpsstepfix.mixin;

import me.jellysquid.mods.sodium.client.gui.SodiumGameOptionPages;
import me.jellysquid.mods.sodium.client.gui.options.Option;
import me.jellysquid.mods.sodium.client.gui.options.control.ControlValueFormatter;
import me.jellysquid.mods.sodium.client.gui.options.control.SliderControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium 0.5.x (la version para MC 1.20.1) crea el slider de limite de FPS en
 * {@code SodiumGameOptionPages.general()} (dentro de un lambda que se pasa a
 * {@code OptionImpl.Builder#setControl}) con exactamente:
 *
 *     new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit())
 *
 * El ultimo "10" es el step (interval) del slider. Verificado contra el
 * bytecode real de sodium-fabric-0.5.13+mc1.20.1.jar (lambda$general$14:
 * bipush 10, sipush 260, bipush 10). Esta version NO tiene la API
 * net.caffeinemc.mods.sodium.api.config.structure (SodiumConfigBuilder /
 * IntegerOptionBuilder#setRange) que usa Sodium 0.6+; SliderControl recibe
 * min/max/interval directamente en su constructor.
 *
 * Este mixin intercepta TODAS las construcciones de SliderControl dentro de
 * SodiumGameOptionPages (sin anclarse a la posicion ni al nombre del lambda,
 * que cambia entre builds) y, identificando el slider de FPS unicamente por
 * sus VALORES (min == 10 && max == 260 && interval == 10), cambia el interval
 * a 1. Ningun otro slider de Sodium (mipmaps 0-4-1, etc.) coincide con ese
 * rango, asi que quedan intactos.
 *
 * El valor elegido se escribe via Option#setValue -> binding vanilla
 * (Options.framerateLimit), por lo que el FPS exacto elegido se respeta y
 * persiste (junto con OptionsMixin, que elimina el reescalado *10//10).
 */
@Mixin(SodiumGameOptionPages.class)
public class SliderControlMixin {

    @Redirect(
            method = "*",
            at = @At(
                    value = "NEW",
                    target = "(Lme/jellysquid/mods/sodium/client/gui/options/Option;IIILme/jellysquid/mods/sodium/client/gui/options/control/ControlValueFormatter;)Lme/jellysquid/mods/sodium/client/gui/options/control/SliderControl;"
            )
    )
    private static SliderControl sodiumFpsStepFix$fixFpsSliderStep(
            Option<Integer> option, int min, int max, int interval, ControlValueFormatter formatter
    ) {
        if (min == 10 && max == 260 && interval == 10) {
            interval = 1; // Cambiar el step a 1 solo en el slider de FPS (10-260).
        }

        return new SliderControl(option, min, max, interval, formatter);
    }
}
