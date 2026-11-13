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
 * POR QUE AQUI Y NO EN SodiumConfigBuilder (mismo cambio aplicado antes en 26.3):
 * El mixin anterior (SodiumConfigBuilderMixin) se anclaba al metodo privado
 * SodiumConfigBuilder#buildGeneralPage y redirigia su llamada a
 * IntegerOptionBuilder#setRange(10, 260, 10). Eso funciona en las Sodium de la
 * linea 26.2, pero es fragil por dos motivos:
 *
 *   1) Depende de la FIRMA de un metodo interno de Sodium. En la linea 26.3 esa
 *      firma cambio (buildGeneralPage paso a recibir un VanillaOptions extra) y
 *      dejo de llamar a setRange para el limite de FPS; con "defaultRequire": 1
 *      en el mixins.json el resultado fue un crash al iniciar el juego.
 *   2) Depende de que la opcion siga llamando a setRange dentro de ese metodo.
 *
 * En cambio, net.caffeinemc.mods.sodium.api.config.option.Range es API publica y
 * conserva la misma forma en todas las Sodium de la linea 26.2 (verificado con
 * javap sobre los jars reales):
 *
 *   - mc26.2-0.9.1-fabric / -neoforge (pin de compilacion de esta rama)
 *   - mc26.2-0.9.2-fabric / -neoforge (instalados)
 *
 *     -> record Range(int min, int max, int step) con min()/max()/step()
 *
 * y en todas ellas SliderControl$SliderControlElement lee la granularidad del
 * slider con IntegerOption#getSteppedValidator().step(), asi que devolver 1 aqui
 * basta para que el slider permita cualquier FPS (144 incluido):
 *
 *   buildGeneralPage -> setRange(10, 260, 10)
 *                    -> IntegerOptionBuilderImpl.setRange(int,int,int)
 *                    -> new Range(10, 260, 10)   <- este mixin lo deja en 1
 *
 * El guard (min == 10 && max == 260) es quirurgico: en la linea 26.2 el unico
 * setRange con esas constantes es el del framerate limit (bci 920 de
 * buildGeneralPage en 0.9.1 y 0.9.2); el resto de opciones (GUI scale, brightness,
 * etc.) pasa intacto.
 *
 * No se usa @Redirect/@At INVOKE ni el nombre de ningun metodo interno de Sodium:
 * se injecta en el accessor step() del propio record, de modo que el mixin no
 * puede quedar huerfano si Sodium reorganiza sus paginas de configuracion (que es
 * justo lo que le paso a la linea 26.3 con 0.9.3).
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
