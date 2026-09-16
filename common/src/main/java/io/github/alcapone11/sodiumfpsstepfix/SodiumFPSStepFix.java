package io.github.alcapone11.sodiumfpsstepfix;

// MC 1.21.1 (Mojang mappings): ResourceLocation with
// ResourceLocation.fromNamespaceAndPath(...). Verified by successful
// compilation against both loom and ModDevGradle artifacts.
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SodiumFPSStepFix {
	public static final String MOD_ID = "sodium-fps-step-fix";
	public static final String NEOFORGE_MOD_ID = "sodiumfpsstepfix";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private SodiumFPSStepFix() {
	}

	public static void init() {
		// This code runs as soon as Minecraft is in a mod-load-ready state.
		// However, some things (like resources) may still be uninitialized.
		// Proceed with mild caution.

		LOGGER.info("Sodium FPS Step Fix loaded.");
	}

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}
}
