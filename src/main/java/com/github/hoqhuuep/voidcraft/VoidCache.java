package com.github.hoqhuuep.voidcraft;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockType;
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

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

public class VoidCache {
	private static final int SEA_LEVEL = 62;
	private static final int WORLD_HEIGHT = 256;
	private static final int CACHE_EXPIRY_SECONDS = 60;

	private final BiomeGenerator biomeGenerator;
	private final GenerationPopulator baseGenerator;
	private final List<GenerationPopulator> generationPopulators;

	public VoidCache(BiomeGenerator biomeGenerator, GenerationPopulator baseGenerator,
			List<GenerationPopulator> generationPopulators) {
		this.biomeGenerator = biomeGenerator;
		this.baseGenerator = baseGenerator;
		this.generationPopulators = generationPopulators;
	}

	public boolean[][] getOceanBiome(World world, int chunkX, int chunkZ) {
		return oceanBiomeCache.getUnchecked(new ChunkCoordinates(world, chunkX, chunkZ));
	}

	public boolean[][] getWaterOrAir(World world, int chunkX, int chunkZ) {
		return waterOrAirCache.getUnchecked(new ChunkCoordinates(world, chunkX, chunkZ));
	}

	private final LoadingCache<ChunkCoordinates, boolean[][]> oceanBiomeCache = CacheBuilder.newBuilder()
			.expireAfterAccess(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
			.build(new CacheLoader<ChunkCoordinates, boolean[][]>() {
				@Override
				public boolean[][] load(ChunkCoordinates key) throws Exception {
					MutableBiomeArea chunkBiomes = biomeCache.getUnchecked(key);
					boolean[][] result = new boolean[16][16];
					for (int z = 0; z < 16; ++z) {
						for (int x = 0; x < 16; ++x) {
							BiomeType biome = chunkBiomes.getBiome(key.chunkX + x, key.chunkZ + z);
							result[z][x] = biome == BiomeTypes.OCEAN || biome == BiomeTypes.DEEP_OCEAN;
						}
					}
					return result;
				}
			});

	private final LoadingCache<ChunkCoordinates, boolean[][]> waterOrAirCache = CacheBuilder.newBuilder()
			.expireAfterAccess(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
			.build(new CacheLoader<ChunkCoordinates, boolean[][]>() {
				@Override
				public boolean[][] load(ChunkCoordinates key) throws Exception {
					MutableBlockVolume chunkBlocks = blockCache.getUnchecked(key);
					boolean[][] result = new boolean[16][16];
					for (int z = 0; z < 16; ++z) {
						for (int x = 0; x < 16; ++x) {
							BlockType block = chunkBlocks.getBlockType(key.chunkX + x, SEA_LEVEL, key.chunkZ + z);
							result[z][x] = block == BlockTypes.AIR || block == BlockTypes.WATER;
						}
					}
					return result;
				}
			});

	private final LoadingCache<ChunkCoordinates, MutableBiomeArea> biomeCache = CacheBuilder.newBuilder()
			.expireAfterAccess(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
			.build(new CacheLoader<ChunkCoordinates, MutableBiomeArea>() {
				@Override
				public MutableBiomeArea load(ChunkCoordinates key) throws Exception {
					MutableBiomeArea chunkBiomes = Sponge.getRegistry().getExtentBufferFactory()
							.createBiomeBuffer(16, 16)
							.getBiomeView(DiscreteTransform2.fromTranslation(key.chunkX, key.chunkZ));
					biomeGenerator.generateBiomes(chunkBiomes);
					return chunkBiomes;
				}
			});

	private final LoadingCache<ChunkCoordinates, MutableBlockVolume> blockCache = CacheBuilder.newBuilder()
			.expireAfterAccess(CACHE_EXPIRY_SECONDS, TimeUnit.SECONDS)
			.build(new CacheLoader<ChunkCoordinates, MutableBlockVolume>() {
				@Override
				public MutableBlockVolume load(ChunkCoordinates key) throws Exception {
					MutableBiomeArea chunkBiomes = biomeCache.getUnchecked(key);

					// Generate base terrain
					MutableBlockVolume chunkBlocks = Sponge.getRegistry().getExtentBufferFactory()
							.createBlockBuffer(16, WORLD_HEIGHT, 16)
							.getBlockView(DiscreteTransform3.fromTranslation(key.chunkX, 0, key.chunkZ));
					ImmutableBiomeArea biomeBuffer = chunkBiomes.getImmutableBiomeCopy();
					baseGenerator.populate(key.world, chunkBlocks, biomeBuffer);

					// Apply the generator populators to complete the
					// blockBuffer
					for (GenerationPopulator populator : generationPopulators) {
						if (populator.getClass() != VoidCraftGenerator.class) {
							populator.populate(key.world, chunkBlocks, biomeBuffer);
						}
					}

					// Get unique biomes to determine what generator populators
					// to run
					List<BiomeType> uniqueBiomes = Lists.newArrayList();
					for (int x = 0; x < 16; x++) {
						for (int z = 0; z < 16; z++) {
							BiomeType biome = chunkBiomes.getBiome(key.chunkX + x, key.chunkZ + z);
							if (!uniqueBiomes.contains(biome)) {
								uniqueBiomes.add(biome);
							}
						}
					}

					// Apply biome specific generator populators
					for (BiomeType type : uniqueBiomes) {
						for (GenerationPopulator populator : key.world.getWorldGenerator().getBiomeSettings(type)
								.getGenerationPopulators()) {
							if (populator.getClass() != VoidCraftGenerator.class) {
								populator.populate(key.world, chunkBlocks, biomeBuffer);
							}
						}
					}

					return chunkBlocks;
				}
			});

	private static class ChunkCoordinates {
		private final World world;
		private final int chunkX;
		private final int chunkZ;

		public ChunkCoordinates(World world, int chunkX, int chunkZ) {
			this.world = world;
			this.chunkX = chunkX;
			this.chunkZ = chunkZ;
		}

		@Override
		public int hashCode() {
			return (chunkX << 16) ^ chunkZ ^ world.hashCode();
		}

		@Override
		public boolean equals(Object that) {
			if (that == null || that.getClass() != getClass()) {
				return false;
			}
			ChunkCoordinates other = (ChunkCoordinates) that;
			return other.chunkX == chunkX && other.chunkZ == chunkZ && other.world == world;
		}
	}
}
