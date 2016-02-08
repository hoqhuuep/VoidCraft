package com.github.hoqhuuep.voidcraft;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameInitializationEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

@Plugin(id = "voidcraft", name = "VoidCraft", version = "0.1.0")
public class VoidCraftSpongePlugin {
	@Listener
	public void onGameInitialization(GameInitializationEvent event) {
		Sponge.getRegistry().register(WorldGeneratorModifier.class, new VoidCraftGeneratorModifier());
	}
}
