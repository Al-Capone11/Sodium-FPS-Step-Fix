package io.github.alcapone11.sodiumfpsstepfix.mixin;

import net.caffeinemc.mods.sodium.api.config.option.Range;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Fuerza el paso (step) del rango 10-260 de Sodium a 1, es decir, el slider de
 * "Framerate Limit" de las opciones de video de Sodium.
 *
 * POR QUE AQUI Y NO EN SodiumConfigBuilder (historial de 26.3):
 * La version anterior de este mixin se anclaba al metodo privado
 * SodiumConfigBuilder#buildGeneralPage y redirigia su llamada a
 * IntegerOptionBuilder#setRange(10, 260, 10). Eso funcionaba hasta Sodium
 * 0.9.3-alpha.1, pero en 0.9.3-beta.1 Sodium cambio la firma del metodo
 * (ahora es buildGeneralPage(ConfigBuilder, VanillaOptions)) y dejo de llamar
 * a setRange para el limite de FPS: pasa a derivar min/max/step del propio
 * OptionInstance vanilla (VanillaOption#getRangeFor). Con el selector de
 * metodo ya inexistente y "defaultRequire": 1 en el mixins.json, el resultado
 * fue InvalidInjectionException al iniciar el juego.
 *
 * En cambio, net.caffeinemc.mods.sodium.api.config.option.Range es API publica
 * y conserva EXACTAMENTE la misma forma en las tres versiones de Sodium para
 * 26.3 (verificado con javap sobre los jars reales):
 *
 *   - mc26.3-0.9.2-fabric        -> record Range(int min, int max, int step)
 *   - mc26.3-0.9.3-alpha.1-fabric-> record Range(int min, int max, int step)
 *   - mc26.3-0.9.3-beta.1-fabric -> record Range(int min, int max, int step)
 *
 * en las tres, SliderControl$SliderControlElement lee la granularidad del
 * slider con IntegerOption#getSteppedValidator().step(), asi que devolver 1
 * aqui basta para que el slider permita cualquier FPS (144 incluido) con
 * independencia de quien construya el rango:
 *
 *   0.9.2 / 0.9.3-alpha.1: buildGeneralPage -> setRange(10, 260, 10)
 *                         -> IntegerOptionBuilderImpl.setRange(int,int,int)
 *                         -> new Range(10, 260, 10)   <- este mixin lo deja en 1
 *   0.9.3-beta.1:          VanillaOptions -> VanillaOption#getRangeFor
 *                         -> new Range(10, 260, step del OptionInstance)
 *                         (con OptionsMixin esa step ya es 1; devolver 1 es
 *                          idempotente)
 *
 * El guard (min == 10 && max == 260) es quirurgico: en 0.9.2 el unico
 * setRange con esas constantes es el del framerate limit (bci 924 de
 * buildGeneralPage) y en 0.9.3-beta.1 el unico setRange(III) restante es el de
 * buildPerformancePage, con valores distintos (iconst_1), asi que no lo toca.
 *
 * No se usa @Redirect/@At INVOKE ni el nombre de ningun metodo interno de
 * Sodium: se injecta en el accessor step() del propio record, de modo que el
 * mixin no puede volver a quedar huerfano si Sodium reorganiza sus paginas de
 * configuracion.
 */
@Mixin(Range.class)
public abstract class SodiumRangeMixin {

    @Shadow
    public abstract int min();

    @Shadow
    public abstract int max();

    @Inject(method = "step()I", at = @At("HEAD"), cancellable = true)
    private void sodiumFpsStepFix$forceStepOne(CallbackInfoReturnable<Integer> cir) {
        if (this.min() == 10 && this.max() == 260) {
            // Range del limite de FPS: permitir cualquier valor (paso 1).
            cir.setReturnValue(1);
        }
    }
}
