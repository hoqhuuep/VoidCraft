package com.github.hoqhuuep.voidcraft;

import org.spongepowered.api.block.BlockType;

public interface BlockSetter {
	void onBlockSet(int x, int y, int z, BlockType type);
}
