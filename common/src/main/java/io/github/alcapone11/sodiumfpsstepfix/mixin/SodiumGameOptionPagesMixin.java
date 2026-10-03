package io.github.alcapone11.sodiumfpsstepfix.mixin;

import net.caffeinemc.mods.sodium.client.gui.SodiumGameOptionPages;
import net.caffeinemc.mods.sodium.client.gui.options.Option;
import net.caffeinemc.mods.sodium.client.gui.options.control.ControlValueFormatter;
import net.caffeinemc.mods.sodium.client.gui.options.control.SliderControl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Sodium 0.7.x (el que corresponde a MC 1.21.10) ya NO tiene
 * SodiumConfigBuilder ni la API api.config.structure de 0.8.x. Verificado
 * con javap contra ambos jars reales (fabric remapeado por loom y el
 * jarjar interno del neoforge):
 *
 *   - net/caffeinemc/mods/sodium/client/gui/SodiumConfigBuilder.class  -> NO EXISTE en 0.7.3
 *   - net/caffeinemc/mods/sodium/api/config/structure/...              -> NO EXISTE en 0.7.3
 *
 * El slider de limite de FPS vive en SodiumGameOptionPages.general(): la
 * opcion se crea con el nombre "options.framerateLimit" y su control se
 * construye en el lambda sintetico lambda$general$20 como:
 *
 *   new SliderControl(option, 10, 260, 10, ControlValueFormatter.fpsLimit())
 *
 * que es exactamente el mismo (min=10, max=260, step=10) que la version
 * 0.8.x pasaba a IntegerOptionBuilder.setRange(10, 260, 10).
 *
 * El bytecode de SodiumGameOptionPages contiene 11 construcciones de
 * SliderControl (fps, guiScale, biomeBlend, chunkBuilderThreads, etc.),
 * pero la combinacion (10, 260, 10) es UNICA: solo el slider de FPS.
 *
 * EN lugar de apuntar al lambda por su nombre ("lambda$general$20", que
 * javac renumera si Sodium agrega o quita opciones), usamos method = "*":
 * el handler se aplica a TODAS las instrucciones NEW de SliderControl en la
 * clase y el guard de valores identifica la del FPS, igual que hacia el
 * mixin original de 0.8.x. Si Sodium cambia los limites del slider, el
 * defaultRequire=1 del mixins.json hara fallar el mixin en runtime de
 * forma audible (nada de fallos silenciosos).
 *
 * NOTA SOBRE EL PRIMER INTENTO (fallido en runtime, crash al abrir opciones
 * de video en AMBOS loaders): @ModifyArgs + @At("INVOKE") sobre
 * SliderControl.<init> con method="*" escaneo 0 targets. El patron que SI
 * funciona (verificado in-game en el port 1.20.1 de este mod) es @Redirect
 * con @At("NEW").
 *
 * VERIFICADO PARA SODIUM 0.7.3 / MC 1.21.10 (2026-10-03):
 *   - fabric (loom remap):  lambda$general$20 -> bipush 10, sipush 260, bipush 10
 *   - neoforge (jarjar interno net.caffeinemc.sodium-neoforge-0.7.3+mc1.21.10-mod.jar):
 *     misma clase, mismo lambda, mismas constantes.
 *   - Constructor publico: SliderControl(Option<Integer>, int min, int max,
 *     int interval, ControlValueFormatter).
 */
@Mixin(SodiumGameOptionPages.class)
public class SodiumGameOptionPagesMixin {

    @Redirect(
            method = "*",
            at = @At(
                    value = "NEW",
                    target = "(Lnet/caffeinemc/mods/sodium/client/gui/options/Option;IIILnet/caffeinemc/mods/sodium/client/gui/options/control/ControlValueFormatter;)Lnet/caffeinemc/mods/sodium/client/gui/options/control/SliderControl;"
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
