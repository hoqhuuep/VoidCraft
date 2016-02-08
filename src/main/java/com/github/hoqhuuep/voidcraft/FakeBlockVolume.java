package com.github.hoqhuuep.voidcraft;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockType;
import org.spongepowered.api.util.DiscreteTransform3;
import org.spongepowered.api.world.extent.ImmutableBlockVolume;
import org.spongepowered.api.world.extent.MutableBlockVolume;
import org.spongepowered.api.world.extent.StorageType;
import org.spongepowered.api.world.extent.UnmodifiableBlockVolume;

import com.flowpowered.math.vector.Vector3i;

public class FakeBlockVolume implements MutableBlockVolume {
	private final Vector3i blockMin;
	private final Vector3i blockMax;
	private final BlockSetter blockSetter;

	public FakeBlockVolume(Vector3i blockMin, Vector3i blockMax, BlockSetter blockSetter) {
		this.blockMin = blockMin;
		this.blockMax = blockMax;
		this.blockSetter = blockSetter;
	}

	@Override
	public Vector3i getBlockMin() {
		return blockMin;
	}

	@Override
	public Vector3i getBlockMax() {
		return blockMax;
	}

	@Override
	public Vector3i getBlockSize() {
		return blockMin.sub(blockMax).add(1, 1, 1);
	}

	@Override
	public boolean containsBlock(Vector3i position) {
		return containsBlock(position.getX(), position.getY(), position.getZ());
	}

	@Override
	public boolean containsBlock(int x, int y, int z) {
		return blockMin.getX() <= x && blockMin.getY() <= y && blockMin.getZ() <= z && blockMax.getX() >= x
				&& blockMax.getY() >= y && blockMax.getZ() >= z;
	}

	@Override
	public BlockState getBlock(Vector3i position) {
		return null;
	}

	@Override
	public BlockState getBlock(int x, int y, int z) {
		return null;
	}

	@Override
	public BlockType getBlockType(Vector3i position) {
		return null;
	}

	@Override
	public BlockType getBlockType(int x, int y, int z) {
		return null;
	}

	@Override
	public UnmodifiableBlockVolume getUnmodifiableBlockView() {
		return null;
	}

	@Override
	public MutableBlockVolume getBlockCopy() {
		return null;
	}

	@Override
	public MutableBlockVolume getBlockCopy(StorageType type) {
		return null;
	}

	@Override
	public ImmutableBlockVolume getImmutableBlockCopy() {
		return null;
	}

	@Override
	public void setBlock(Vector3i position, BlockState block) {
		blockSetter.onBlockSet(position.getX(), position.getY(), position.getZ(), block.getType());
	}

	@Override
	public void setBlock(int x, int y, int z, BlockState block) {
		blockSetter.onBlockSet(x, y, z, block.getType());
	}

	@Override
	public void setBlockType(Vector3i position, BlockType type) {
		blockSetter.onBlockSet(position.getX(), position.getY(), position.getZ(), type);
	}

	@Override
	public void setBlockType(int x, int y, int z, BlockType type) {
		blockSetter.onBlockSet(x, y, z, type);
	}

	@Override
	public MutableBlockVolume getBlockView(Vector3i newMin, Vector3i newMax) {
		return null;
	}

	@Override
	public MutableBlockVolume getBlockView(DiscreteTransform3 transform) {
		return null;
	}

	@Override
	public MutableBlockVolume getRelativeBlockView() {
		return null;
	}
}
