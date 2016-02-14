package com.github.hoqhuuep.voidcraft;

import java.util.List;

import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.world.WorldCreationSettings;
import org.spongepowered.api.world.biome.BiomeTypes;
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
		List<GenerationPopulator> previousGenerationPopulators = worldGenerator.getGenerationPopulators();
		BiomeGenerator biomeGenerator = worldGenerator.getBiomeGenerator();
		GenerationPopulator voidCraftGenerator = new VoidCraftGenerator(baseGenerator, previousGenerationPopulators, biomeGenerator);
		worldGenerator.getGenerationPopulators().add(voidCraftGenerator);
		
		// Explicitly add to mesa biomes... mesa's have their own GenerationPopulators which conflict which override
		worldGenerator.getBiomeSettings(BiomeTypes.MESA).getGenerationPopulators().add(voidCraftGenerator);
		worldGenerator.getBiomeSettings(BiomeTypes.MESA_PLATEAU_FOREST).getGenerationPopulators().add(voidCraftGenerator);
		worldGenerator.getBiomeSettings(BiomeTypes.MESA_PLATEAU).getGenerationPopulators().add(voidCraftGenerator);
		worldGenerator.getBiomeSettings(BiomeTypes.MESA_BRYCE).getGenerationPopulators().add(voidCraftGenerator);
		worldGenerator.getBiomeSettings(BiomeTypes.MESA_PLATEAU_FOREST_MOUNTAINS).getGenerationPopulators().add(voidCraftGenerator);
		worldGenerator.getBiomeSettings(BiomeTypes.MESA_PLATEAU_MOUNTAINS).getGenerationPopulators().add(voidCraftGenerator);
	}
}
