package io.github.foundationgames.phonos.client.render.block;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.RotationAxis;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

public class EnderMusicBoxBlockEntityRenderer extends CableOutputBlockEntityRenderer<EnderMusicBoxBlockEntity> {

    public static final String[] ANIM = new String[] {".", "o", "O"};

    public static final int TEXT_COLOR = 0x27C4A9;

    private final TextRenderer font;

    public EnderMusicBoxBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);

        this.font = ctx.getTextRenderer();
    }

    @Override
    public void render(EnderMusicBoxBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        if (!entity.playingClient)
            return;

        matrices.push();

        matrices.translate(0.5, 0.5, 0.5);
        matrices.translate(0, 0.501, 0);

        matrices.multiply(RotationAxis.POSITIVE_X.rotation((float) Math.PI / 2));

        int animIndex = (int) ((entity.getWorld().getTime() / 4) % 4);
        {
            // Render quad
            var vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(Phonos.id("textures/entity/ender_music_box_animated.png")));

            Matrix4f matrix4f = matrices.peek().getPositionMatrix();
            Matrix3f matrix3f = matrices.peek().getNormalMatrix();

            vc.vertex(matrix4f, -0.5f, -0.5f, 0)
                .color(255, 255, 255, 255)
                .texture(0, animIndex * 0.25f)
                .overlay(overlay)
                .light(15728880)
                .normal(matrix3f, 0, 1, 0)
                .next();

            vc.vertex(matrix4f, -0.5f, 0.5f, 0)
                .color(255, 255, 255, 255)
                .texture(0, (1 + animIndex) * 0.25f)
                .overlay(overlay)
                .light(15728880)
                .normal(matrix3f, 0, 1, 0)
                .next();

            vc.vertex(matrix4f, 0.5f, 0.5f, 0)
                .color(255, 255, 255, 255)
                .texture(1, (1 + animIndex) * 0.25f)
                .overlay(overlay)
                .light(15728880)
                .normal(matrix3f, 0, 1, 0)
                .next();

            vc.vertex(matrix4f, 0.5f, -0.5f, 0)
                .color(255, 255, 255, 255)
                .texture(1, animIndex * 0.25f)
                .overlay(overlay)
                .light(15728880)
                .normal(matrix3f, 0, 1, 0)
                .next();
        }

        matrices.pop();
    }
}
