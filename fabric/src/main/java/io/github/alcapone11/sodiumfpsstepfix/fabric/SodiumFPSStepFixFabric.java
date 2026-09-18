package io.github.alcapone11.sodiumfpsstepfix.fabric;

import io.github.alcapone11.sodiumfpsstepfix.SodiumFPSStepFix;
import net.fabricmc.api.ModInitializer;

public class SodiumFPSStepFixFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        SodiumFPSStepFix.init();
    }
}
