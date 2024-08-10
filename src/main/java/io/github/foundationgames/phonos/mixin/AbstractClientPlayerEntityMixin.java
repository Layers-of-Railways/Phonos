package io.github.foundationgames.phonos.mixin;

import com.mojang.authlib.GameProfile;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingPlayerEntity;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(AbstractClientPlayerEntity.class)
public abstract class AbstractClientPlayerEntityMixin extends PlayerEntity implements IMicrophoneHoldingPlayerEntity {
    @Unique
    private boolean phonos$isHolding = false;

    @Unique
    private int phonos$armPoseOverrideExpiration = 0;

    private AbstractClientPlayerEntityMixin(World world, BlockPos pos, float yaw, GameProfile gameProfile) {
        super(world, pos, yaw, gameProfile);
    }

    @Override
    public void phonos$setHolding(boolean holding) {
        phonos$isHolding = holding;
        phonos$armPoseOverrideExpiration = this.age + 2;
    }

    @Override
    public boolean phonos$isHolding() {
        if (this.age > phonos$armPoseOverrideExpiration) {
            phonos$isHolding = false;
        }
        return phonos$isHolding;
    }
}
