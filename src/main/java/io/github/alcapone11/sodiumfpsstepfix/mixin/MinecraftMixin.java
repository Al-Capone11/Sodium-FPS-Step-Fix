package io.github.alcapone11.sodiumfpsstepfix.mixin;

import io.github.alcapone11.sodiumfpsstepfix.PreciseFrameLimiter;
import net.minecraft.client.FramerateLimiter;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Options;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(Minecraft.class)
public class MinecraftMixin {
    @Unique
    private final PreciseFrameLimiter sodiumFpsStepFix$preciseFrameLimiter = new PreciseFrameLimiter();

    @Shadow
    @Final
    public Options options;

    @Redirect(
            method = "renderFrame(Z)V",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/client/FramerateLimiter;limitDisplayFPS(I)V"
            )
    )
    private void sodiumFpsStepFix$usePreciseFrameLimiter(int framerateLimit) {
        if (this.options.enableVsync().get()) {
            this.sodiumFpsStepFix$preciseFrameLimiter.reset();
            FramerateLimiter.limitDisplayFPS(framerateLimit);
            return;
        }

        this.sodiumFpsStepFix$preciseFrameLimiter.awaitNextFrame(framerateLimit);
    }
}
