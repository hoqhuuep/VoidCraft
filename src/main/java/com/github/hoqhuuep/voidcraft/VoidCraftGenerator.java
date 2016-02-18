package com.github.hoqhuuep.voidcraft;

import java.util.List;

import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.GenerationPopulator;

import com.flowpowered.noise.Noise;
import com.flowpowered.noise.NoiseQuality;

public class VoidCraftGenerator implements GenerationPopulator {
	private static final int SEA_LEVEL = 62;
	private final VoidCache cache;

	public VoidCraftGenerator(GenerationPopulator baseGenerator, List<GenerationPopulator> generationPopulators,
			BiomeGenerator biomeGenerator) {
		cache = new VoidCache(biomeGenerator, baseGenerator, generationPopulators);
	}

	@Override
	public void populate(World world, MutableBlockVolume buffer, ImmutableBiomeArea biomes) {
		int noiseSeed = (int) world.getCreationSettings().getSeed();
		final int xMin = buffer.getBlockMin().getX();
		final int zMin = buffer.getBlockMin().getZ();
		final int xMax = buffer.getBlockMax().getX() + 1;
		final int zMax = buffer.getBlockMax().getZ() + 1;
		int[] yMax = new int[(xMax - xMin) * (zMax - zMin)];

		// We need to check a 5x5 chunk area for ocean biomes...
		boolean anyOcean = false;
		boolean[][] ocean = new boolean[80][80];
		for (int chunkZ = zMin - 32; chunkZ < zMax + 32; chunkZ += 16) {
			for (int chunkX = xMin - 32; chunkX < xMax + 32; chunkX += 16) {
				boolean[][] oceanBiomeChunk = cache.getOceanBiome(world, chunkX, chunkZ);
				for (int z = 0; z < 16; ++z) {
					for (int x = 0; x < 16; ++x) {
						if (oceanBiomeChunk[z][x]) {
							anyOcean = true;
							ocean[chunkZ + z - zMin + 32][chunkX + x - xMin + 32] = true;
						}
					}
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
					boolean[][] waterOrAirChunk = cache.getWaterOrAir(world, chunkX, chunkZ);
					for (int z = 0; z < 16; ++z) {
						for (int x = 0; x < 16; ++x) {
							if (distanceToOcean[chunkZ + z - zMin + 32][chunkX + x - xMin + 32] >= 16.0) {
								continue;
							}
							if (waterOrAirChunk[z][x]) {
								anySky = true;
								sky[chunkZ + z - zMin + 16][chunkX + x - xMin + 16] = true;
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
}
