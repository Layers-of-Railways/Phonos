package io.github.foundationgames.phonos.mixin.client;

import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingClientPlayerEntity;
import net.minecraft.client.model.ModelPart;
import net.minecraft.client.render.entity.model.BipedEntityModel;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BipedEntityModel.class)
public class BipedEntityModelMixin<T extends LivingEntity> {
    @Shadow @Final public ModelPart rightArm;

    @Shadow @Final public ModelPart head;

    @Shadow @Final public ModelPart leftArm;

    @Inject(method = "positionLeftArm", at = @At("HEAD"), cancellable = true)
    private void phonos$positionLeftArm(T entity, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player && entity instanceof IMicrophoneHoldingClientPlayerEntity microphoneHolding) {
            if (player.getMainArm() != Arm.LEFT || !microphoneHolding.phonos$isHolding())
                return;

            this.leftArm.pitch = /*MathHelper.clamp(*/this.head.pitch - 1.9198622f - (entity.isInSneakingPose() ? 0.2617994f : 0.0f)/*, -2.4f, 3.3f)*/;
            this.leftArm.yaw = this.head.yaw + 0.2617994f*2.2f;
            this.leftArm.pitch += 0.5f;
//            this.leftArm.roll -= 0.8f;
            ci.cancel();
        }
    }

    @Inject(method = "positionRightArm", at = @At("HEAD"), cancellable = true)
    private void phonos$positionRightArm(T entity, CallbackInfo ci) {
        if (entity instanceof PlayerEntity player && entity instanceof IMicrophoneHoldingClientPlayerEntity microphoneHolding) {
            if (player.getMainArm() != Arm.RIGHT || !microphoneHolding.phonos$isHolding())
                return;

            this.rightArm.pitch = /*MathHelper.clamp(*/this.head.pitch - 1.9198622f - (entity.isInSneakingPose() ? 0.2617994f : 0.0f)/*, -2.4f, 3.3f)*/;
            this.rightArm.yaw = this.head.yaw - 0.2617994f*2.2f;
            this.rightArm.pitch += 0.5f;
            ci.cancel();
        }
    }
}
