package io.github.foundationgames.phonos.mixin.client;

import io.github.foundationgames.phonos.client.model.PhonosPartialModels;
import io.github.foundationgames.phonos.client.render.block.MicrophoneBaseBlockEntityRenderer;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.item.HeldItemRenderer;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Arm;
import net.minecraft.util.math.MathHelper;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(HeldItemRenderer.class)
public abstract class HeldItemRendererMixin {
    @Shadow @Final private MinecraftClient client;

    @Shadow protected abstract void applyEquipOffset(MatrixStack matrices, Arm arm, float equipProgress);

    @Shadow protected abstract void applySwingOffset(MatrixStack matrices, Arm arm, float swingProgress);

    @Inject(method = "renderArmHoldingItem", at = @At("RETURN"))
    private void phonos$renderMic(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, float equipProgress, float swingProgress, Arm arm, CallbackInfo ci) {
        if (this.client.player instanceof IMicrophoneHoldingPlayerEntity microphoneHolding && microphoneHolding.phonos$isHolding()) {
            RenderLayer renderLayer = TexturedRenderLayers.getEntityTranslucentCull();
            boolean rightHanded = arm == Arm.RIGHT;

            matrices.pop();

            {
                matrices.push();

                float n = -0.4f * MathHelper.sin(MathHelper.sqrt(swingProgress) * (float)Math.PI);
                float m = 0.2f * MathHelper.sin(MathHelper.sqrt(swingProgress) * ((float)Math.PI * 2));
                float f = -0.2f * MathHelper.sin(swingProgress * (float)Math.PI);
                int o = rightHanded ? 1 : -1;
                matrices.translate((float)o * n, m, f);
                this.applyEquipOffset(matrices, arm, equipProgress);
                this.applySwingOffset(matrices, arm, swingProgress);

                {
                    matrices.push();

                    BakedModel model = PhonosPartialModels.MICROPHONE.get();

                    model.getTransformation().getTransformation(rightHanded ? ModelTransformationMode.FIRST_PERSON_RIGHT_HAND : ModelTransformationMode.FIRST_PERSON_LEFT_HAND).apply(!rightHanded, matrices);
                    matrices.translate(-0.5f, -0.5f, -0.5f);

                    MicrophoneBaseBlockEntityRenderer.renderBakedItemModel(model, light, OverlayTexture.DEFAULT_UV, matrices, vertexConsumers.getBuffer(renderLayer));

                    matrices.pop();
                }

                matrices.pop();
            }

            matrices.push();
        }
    }
}
