package io.github.alcapone11.sodiumfpsstepfix.mixin;

import net.caffeinemc.mods.sodium.client.gui.SodiumConfigBuilder;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArgs;
import org.spongepowered.asm.mixin.injection.invoke.arg.Args;

@Mixin(SodiumConfigBuilder.class)
public class SodiumConfigBuilderMixin {
    @ModifyArgs(
        method = "buildGeneralPage",
        at = @At(
            value = "INVOKE",
            target = "Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;setRange(III)Lnet/caffeinemc/mods/sodium/api/config/structure/IntegerOptionBuilder;"
        )
    )
    private void sodiumFpsStepFix$modifyFpsStep(Args args) {
        int min = args.get(0);
        int max = args.get(1);
        int step = args.get(2);

        // Identificar el slider de FPS por sus limites (10 a 260) y su step original (10)
        if (min == 10 && max == 260 && step == 10) {
            args.set(2, 1); // Cambiar el step a 1
        }
    }
}
