package io.github.foundationgames.phonos.client.render.block;

import io.github.foundationgames.phonos.block.entity.RadioTransceiverBlockEntity;
import io.github.foundationgames.phonos.client.model.PhonosPartialModels;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;

import static io.github.foundationgames.phonos.client.render.block.MicrophoneBaseBlockEntityRenderer.renderBakedItemModel;

public class RadioTransceiverBlockEntityRenderer extends CableOutputBlockEntityRenderer<RadioTransceiverBlockEntity> {
    private final TextRenderer font;

    public RadioTransceiverBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);

        this.font = ctx.getTextRenderer();
    }

    @Override
    public void render(RadioTransceiverBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        matrices.push();

        matrices.translate(0.5, 0.5, 0.5);

        matrices.scale(0.0268f, 0.0268f, 0.0268f);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(-entity.getRotation().asRotation()));
        matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(180));

        matrices.translate(0, 7, -19);

        var text = RadioLoudspeakerBlockEntityRenderer.getTextForChannel(entity.getChannel()).asOrderedText();

        this.font.drawWithOutline(text, -this.font.getWidth(text) * 0.5f, 0,
                RadioLoudspeakerBlockEntityRenderer.TEXT_COLOR, RadioLoudspeakerBlockEntityRenderer.OUTLINE_COLOR,
                matrices.peek().getPositionMatrix(), vertexConsumers, 15728880);

        matrices.pop();

        RenderLayer renderLayer = TexturedRenderLayers.getEntityCutout();
        matrices.push();

        matrices.translate(0, entity.getTransmissionTowerHeight(), 0);
        matrices.translate(0.5, 0.5, 0.5);
        matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180 - entity.getRotation().asRotation()));
        matrices.translate(-0.5, -0.5, -0.5);

        renderBakedItemModel(PhonosPartialModels.RADIO_ANTENNA.get(), light, overlay, matrices, vertexConsumers.getBuffer(renderLayer));

        matrices.pop();
    }
}
