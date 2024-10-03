package io.github.foundationgames.phonos.client.render.block;

import io.github.foundationgames.phonos.block.MicrophoneBaseBlock;
import io.github.foundationgames.phonos.block.entity.WirelessMicrophoneBaseBlockEntity;
import io.github.foundationgames.phonos.client.model.PartialModel;
import io.github.foundationgames.phonos.client.model.PhonosPartialModels;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingClientPlayerEntity;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.RotationAxis;

import java.util.UUID;

import static io.github.foundationgames.phonos.client.render.block.MicrophoneBaseBlockEntityRenderer.renderBakedItemModel;
import static io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingClientPlayerEntity.State.WIRELESS;

public class WirelessMicrophoneBaseBlockEntityRenderer extends CableOutputBlockEntityRenderer<WirelessMicrophoneBaseBlockEntity> {
    public WirelessMicrophoneBaseBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(WirelessMicrophoneBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        Direction facing = entity.getCachedState().get(MicrophoneBaseBlock.FACING);
        MinecraftClient mc = MinecraftClient.getInstance();

        UUID clientPlayer = entity.getClientPlayer();
        if (clientPlayer != null && mc.world != null) {
            var player = mc.world.getPlayerByUuid(clientPlayer);
            if (player == null)
                return;

            if (player instanceof IMicrophoneHoldingClientPlayerEntity armPoseOverridable) {
                armPoseOverridable.phonos$setHoldingState(WIRELESS);
            }

            boolean self = player instanceof ClientPlayerEntity;

            RenderLayer renderLayer = TexturedRenderLayers.getEntityTranslucentCull();
            matrices.push();

            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));
            matrices.translate(-0.5, -0.5, -0.5);

            PartialModel model = self
                ? PhonosPartialModels.WIRELESS_MICROPHONE_ON_SELF
                : PhonosPartialModels.WIRELESS_MICROPHONE_ON;
            renderBakedItemModel(model.get(), light, overlay, matrices, vertexConsumers.getBuffer(renderLayer));

            matrices.pop();
        }
    }
}
