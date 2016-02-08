package com.github.hoqhuuep.voidcraft;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.util.DiscreteTransform2;
import org.spongepowered.api.world.World;
import org.spongepowered.api.world.extent.ExtentBufferFactory;
import org.spongepowered.api.world.extent.ImmutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBiomeArea;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.gen.BiomeGenerator;
import org.spongepowered.api.world.gen.GenerationPopulator;

import com.flowpowered.math.vector.Vector3i;
import com.flowpowered.noise.Noise;
import com.flowpowered.noise.NoiseQuality;

public class VoidCraftGenerator implements GenerationPopulator {
	private static final int SEA_LEVEL = 62;
	private static final int SKY_LIMIT = 255;
	private final GenerationPopulator baseGenerator;
	private final BiomeGenerator biomeGenerator;

	public VoidCraftGenerator(GenerationPopulator baseGenerator, BiomeGenerator biomeGenerator) {
		this.baseGenerator = baseGenerator;
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

		// We need to check a 3x3 chunk area to create the smooth edges...
		ExtentBufferFactory bufferFactory = Sponge.getRegistry().getExtentBufferFactory();
		MutableBiomeArea mutableWideBiomes = bufferFactory.createBiomeBuffer(48, 48)
				.getBiomeView(DiscreteTransform2.fromTranslation(xMin - 16, zMax - 16));
		biomeGenerator.generateBiomes(mutableWideBiomes);
		ImmutableBiomeArea wideBiomes = mutableWideBiomes.getImmutableBiomeCopy();

		final boolean[] anySky = new boolean[1];
		final boolean[][] sky = new boolean[48][48];
		BlockSetter skySetter = new BlockSetter() {
			@Override
			public void onBlockSet(int x, int y, int z, BlockType type) {
				if (y == SEA_LEVEL && type != BlockTypes.STONE) {
					anySky[0] = true;
					sky[z - zMin + 16][x - xMin + 16] = true;
				}
			}
		};

		for (int chunkZ = zMin - 16; chunkZ < zMin + 32; chunkZ += 16) {
			for (int chunkX = xMin - 16; chunkX < xMin + 32; chunkX += 16) {
				MutableBlockVolume wideBlocks = new FakeBlockVolume(new Vector3i(chunkX, 0, chunkZ),
						new Vector3i(chunkX + 15, SKY_LIMIT, chunkZ + 15), skySetter);
				baseGenerator.populate(world, wideBlocks, wideBiomes);

				for (int z = chunkZ; z < chunkZ + 16; ++z) {
					for (int x = chunkX; x < chunkX + 16; ++x) {
					}
				}
			}
		}

		double[][] distanceToSky = null;
		if (anySky[0]) {
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
					if (anySky[0]) {
						baseLine = distanceToSky[z - zMin + 16][x - xMin + 16] / 16.0;
						if (baseLine > 1.0) {
							baseLine = 1.0;
						}
					}

					// Inverted mountains underneath land
					double fineNoise = noise(x * 0.16, z * 0.16, 0.8, 1.0, noiseSeed);
					double broadNoise = noise(x * 0.04, z * 0.04, 0.4, 0.95, noiseSeed);

					yMax[i] = (int) (SEA_LEVEL * (1.0 - (baseLine * fineNoise * broadNoise)));
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
