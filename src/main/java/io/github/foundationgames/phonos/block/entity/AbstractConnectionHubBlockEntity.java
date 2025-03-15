package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.OutputBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.math.BlockPos;

public abstract class AbstractConnectionHubBlockEntity extends AbstractOutputBlockEntity implements Syncing, Ticking, OutputBlockEntity {
    public final boolean[] inputs;

    public AbstractConnectionHubBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, BlockConnectionLayout outputLayout, boolean[] inputs) {
        super(type, pos, state, outputLayout);

        this.inputs = inputs;
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        for (int i = 0; i < this.inputs.length; i++) {
            this.inputs[i] = nbt.getBoolean("Input" + i);
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        for (int i = 0; i < this.inputs.length; i++) {
            nbt.putBoolean("Input" + i, this.inputs[i]);
        }
    }
}
