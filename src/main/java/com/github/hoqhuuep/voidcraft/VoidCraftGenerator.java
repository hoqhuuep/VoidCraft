package com.github.hoqhuuep.voidcraft;

import java.util.List;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.util.DiscreteTransform2;
import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.biome.BiomeType;
import org.spongepowered.api.world.biome.BiomeTypes;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.GenerationPopulator;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.NoiseQuality;
import com.google.common.collect.Lists;

public class VoidCraftGenerator implements GenerationPopulator {
	private static final int SEA_LEVEL = 62;
	private static final int WORLD_HEIGHT = 256;
	private final GenerationPopulator baseGenerator;
	private final List<GenerationPopulator> previousGeneratorPopulators;
	private final BiomeGenerator biomeGenerator;

	public VoidCraftGenerator(GenerationPopulator baseGenerator, List<GenerationPopulator> previousGeneratorPopulators,
			BiomeGenerator biomeGenerator) {
		this.baseGenerator = baseGenerator;
		this.previousGeneratorPopulators = previousGeneratorPopulators;
		this.biomeGenerator = biomeGenerator;
	}

	@Override
	public void populate(World world, MutableBlockVolume buffer, ImmutableBiomeArea biomes) {
		int noiseSeed = (int) world.getCreationSettings().getSeed();
		final int xMin = buffer.getBlockMin().getX();
		final int zMin = buffer.getBlockMin().getZ();
		final int xMax = buffer.getBlockMax().getX() + 1;
		final int zMax = buffer.getBlockMax().getZ() + 1;
		int[] yMax = new int[(xMax - xMin) * (zMax - zMin)];

		MutableBiomeArea wideBiomes = Sponge.getRegistry().getExtentBufferFactory().createBiomeBuffer(80, 80)
				.getBiomeView(DiscreteTransform2.fromTranslation(xMin - 32, zMin - 32));
		biomeGenerator.generateBiomes(wideBiomes);
		
		boolean anyOcean = false;
		boolean[][] ocean = new boolean[80][80];
		for (int z = zMin - 32; z < zMax + 32; ++z) {
			for (int x = xMin - 32; x < xMax + 32; ++x) {
				BiomeType biome = wideBiomes.getBiome(x, z);
				if (biome == BiomeTypes.OCEAN || biome == BiomeTypes.DEEP_OCEAN) {
					anyOcean = true;
					ocean[z - zMin + 32][x - xMin + 32] = true;
				}
			}
		}
		double[][] distanceToOcean = null;
		if (anyOcean) {
			distanceToOcean = DistanceTransform.distanceTransform(ocean, true);
		}
		
		// We need to check a 3x3 chunk area to create the smooth edges...
		boolean anySky = false;
		boolean[][] sky = new boolean[48][48];

		if (anyOcean) {
			for (int chunkZ = zMin - 16; chunkZ < zMin + 32; chunkZ += 16) {
				for (int chunkX = xMin - 16; chunkX < xMin + 32; chunkX += 16) {
					MutableBlockVolume originalBlocks = baseGeneration(world, chunkX, chunkZ);
					for (int z = chunkZ; z < chunkZ + 16; ++z) {
						for (int x = chunkX; x < chunkX + 16; ++x) {
							if (distanceToOcean[z - zMin + 32][x - xMin + 32] >= 16.0) {
								continue;
							}
							if (originalBlocks.getBlockType(x, SEA_LEVEL, z) == BlockTypes.WATER) {
								anySky = true;
								sky[z - zMin + 16][x - xMin + 16] = true;
							}
						}
					}
				}
			}
		}

		double[][] distanceToSky = null;
		if (anySky) {
			distanceToSky = DistanceTransform.distanceTransform(sky, true);
		}

		// Work out heights of bottoms of islands...
		int i = 0;
		for (int z = zMin; z < zMax; ++z) {
			for (int x = xMin; x < xMax; ++x) {
				if (sky[z - zMin + 16][x - xMin + 16]) {
					// Remove everything
					yMax[i] = SEA_LEVEL;
				} else {
					double baseLine = 1.0;

					// Smooth edges
					if (anySky) {
						baseLine = distanceToSky[z - zMin + 16][x - xMin + 16] / 16.0;
						if (baseLine > 1.0) {
							baseLine = 1.0;
						}
					}

					// Inverted mountains underneath land
					double noiseValue = noise(x * 0.04, z * 0.04, 0.3, 0.9, noiseSeed);

					yMax[i] = (int) (SEA_LEVEL * (1.0 - (baseLine * noiseValue)));
				}
				++i;
			}
		}

		// Actually replace blocks with air
		i = 0;
		for (int z = zMin; z < zMax; ++z) {
			for (int x = xMin; x < xMax; ++x) {
				for (int y = yMax[i]; y >= 0; --y) {
					buffer.setBlockType(x, y, z, BlockTypes.AIR);
				}
				++i;
			}
		}
	}

	private static double noise(double x, double z, double min, double max, int seed) {
		return min + (max - min) * Noise.valueCoherentNoise3D(x, 0, z, seed, NoiseQuality.STANDARD);
	}

	private MutableBlockVolume baseGeneration(World world, int chunkX, int chunkZ) {
		// TODO reuse both buffers...
		MutableBiomeArea cachedBiomes = Sponge.getRegistry().getExtentBufferFactory().createBiomeBuffer(16, 16)
				.getBiomeView(DiscreteTransform2.fromTranslation(chunkX, chunkZ));
		biomeGenerator.generateBiomes(cachedBiomes);

		// Generate base terrain
		MutableBlockVolume blockBuffer = Sponge.getRegistry().getExtentBufferFactory()
				.createBlockBuffer(16, WORLD_HEIGHT, 16)
				.getBlockView(DiscreteTransform3.fromTranslation(chunkX, 0, chunkZ));
		ImmutableBiomeArea biomeBuffer = cachedBiomes.getImmutableBiomeCopy();
		baseGenerator.populate(world, blockBuffer, biomeBuffer);

		// Apply the generator populators to complete the blockBuffer
		for (GenerationPopulator populator : previousGeneratorPopulators) {
			if (populator.getClass() != getClass()) {
				populator.populate(world, blockBuffer, biomeBuffer);
			}
		}

		// Get unique biomes to determine what generator populators to run
		List<BiomeType> uniqueBiomes = Lists.newArrayList();
		for (int x = 0; x < 16; x++) {
			for (int z = 0; z < 16; z++) {
				BiomeType biome = cachedBiomes.getBiome(chunkX + x, chunkZ + z);
				if (!uniqueBiomes.contains(biome)) {
					uniqueBiomes.add(biome);
				}
			}
		}

		// Apply biome specific generator populators
		for (BiomeType type : uniqueBiomes) {
			for (GenerationPopulator populator : world.getWorldGenerator().getBiomeSettings(type)
					.getGenerationPopulators()) {
				if (populator.getClass() != getClass()) {
					populator.populate(world, blockBuffer, biomeBuffer);
				}
			}
		}

		return blockBuffer;
	}
}
