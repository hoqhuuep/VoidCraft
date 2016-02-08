package com.github.hoqhuuep.voidcraft;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.GenerationPopulator;
import org.spongepowered.api.world.gen.WorldGenerator;
import org.spongepowered.api.world.gen.WorldGeneratorModifier;

public class VoidCraftGeneratorModifier implements WorldGeneratorModifier {
	@Override
	public String getId() {
		return "voidcraft:terrain";
	}

	@Override
	public String getName() {
		return "VoidCraft Terrain";
	}

	@Override
	public void modifyWorldGenerator(WorldCreationSettings world, DataContainer settings,
			WorldGenerator worldGenerator) {
		GenerationPopulator baseGenerator = worldGenerator.getBaseGenerationPopulator();
		BiomeGenerator biomeGenerator = worldGenerator.getBiomeGenerator();
		GenerationPopulator voidCraftGenerator = new VoidCraftGenerator(baseGenerator, biomeGenerator);
		worldGenerator.getGenerationPopulators().add(voidCraftGenerator);
	}
}
