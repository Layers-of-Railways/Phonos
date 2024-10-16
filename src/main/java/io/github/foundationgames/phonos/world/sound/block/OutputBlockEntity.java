package io.github.foundationgames.phonos.world.sound.block;

import io.github.foundationgames.phonos.client.render.CableVBOContainer;
import io.github.foundationgames.phonos.sound.emitter.ForwardingSoundEmitter;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public interface OutputBlockEntity extends ForwardingSoundEmitter {
    boolean canConnect(ItemUsageContext ctx);

    boolean addConnection(Vec3d hitPos, @Nullable DyeColor color, InputPlugPoint destInput, ItemStack cable);

    BlockEntityOutputs getOutputs();

    void enforceVBOState(boolean enabled);

    CableVBOContainer getOrCreateVBOContainer();

    default Direction getRotation() {
        return Direction.NORTH;
    }
}
