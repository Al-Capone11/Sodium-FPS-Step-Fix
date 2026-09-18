package io.github.alcapone11.sodiumfpsstepfix;

// MC 26.2: Identifier lives in net.minecraft.resources.Identifier
// (used by both the fabric and neoforge subprojects in this multi-project
// setup; MC 26.x is unobfuscated, so this is the runtime name as well).
import net.minecraft.resources.Identifier;

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

	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}
}
