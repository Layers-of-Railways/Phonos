package io.github.foundationgames.phonos.client.render.block;

import io.github.foundationgames.phonos.block.MicrophoneBaseBlock;
import io.github.foundationgames.phonos.block.entity.MicrophoneBaseBlockEntity;
import io.github.foundationgames.phonos.client.model.PhonosPartialModels;
import io.github.foundationgames.phonos.client.render.CableRenderer;
import io.github.foundationgames.phonos.config.PhonosClientConfig;
import io.github.foundationgames.phonos.mixin.client.WorldRendererAccess;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingClientPlayerEntity;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.Pose3f;
import io.github.foundationgames.phonos.world.sound.CablePlugPoint;
import io.github.foundationgames.phonos.world.sound.MutableCablePlugPoint;
import io.github.foundationgames.phonos.world.sound.RenderableCableConnection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRendererFactory;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.BakedQuad;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.random.Random;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.UUID;

import static io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingClientPlayerEntity.State.WIRED;

public class MicrophoneBaseBlockEntityRenderer extends CableOutputBlockEntityRenderer<MicrophoneBaseBlockEntity> {
    private MutableCablePlugPoint start;
    private MutableCablePlugPoint end;
    private final RenderableCableConnection conn = new RenderableCableConnection() {
        @Override
        public CablePlugPoint getStart() {
            return start;
        }

        @Override
        public CablePlugPoint getEnd() {
            return end;
        }

        @Override
        public @Nullable DyeColor getColor() {
            return null;
        }

        @Override
        public boolean isStatic() {
            return false;
        }
    };

    public MicrophoneBaseBlockEntityRenderer(BlockEntityRendererFactory.Context ctx) {
        super(ctx);
    }

    @Override
    public void render(MicrophoneBaseBlockEntity entity, float tickDelta, MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, int overlay) {
        super.render(entity, tickDelta, matrices, vertexConsumers, light, overlay);

        Vector3f beCenter = new Vector3f(entity.getPos().getX() + 0.5f, entity.getPos().getY() + 0.5f, entity.getPos().getZ() + 0.5f);
        Direction rotation = entity.getCachedState().get(MicrophoneBaseBlock.FACING);
        if (start == null || end == null) {
            start = new MutableCablePlugPoint(
                new Pose3f(
                    new Vector3f(0 / 16.0f, -5 / 16.0f, -3 / 16.0f),
                    PhonosUtil.rotationTo(Direction.NORTH)
                ),
                new Pose3f(
                    beCenter,
                    PhonosUtil.rotationTo(rotation)
                )
            );
            end = new MutableCablePlugPoint(
                new Pose3f(
                    new Vector3f(0 / 16.0f, -5 / 16.0f, -3 / 16.0f)
                        .add(beCenter),
                    PhonosUtil.rotationTo(Direction.NORTH)
                ),
                new Pose3f(
                    new Vector3f(0, 0, 0),
                    new Quaternionf()
                )
            );
        }

        Direction facing = entity.getCachedState().get(MicrophoneBaseBlock.FACING);
        MinecraftClient mc = MinecraftClient.getInstance();

        UUID clientPlayer = entity.getClientPlayer();
        if (clientPlayer == null) {
            RenderLayer renderLayer = TexturedRenderLayers.getEntityTranslucentCull();
            matrices.push();

            matrices.translate(0, 6 / 16.0f, 0);
            matrices.translate(0.5, 0.5, 0.5);
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(facing.asRotation()));
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees((facing.getAxis() == Axis.X ? -1 : 1) * 67.5f));
            matrices.translate(-0.5, -0.5, -0.5);

            renderBakedItemModel(PhonosPartialModels.MICROPHONE.get(), light, overlay, matrices, vertexConsumers.getBuffer(renderLayer));

            matrices.pop();
        } else {
            if (mc.world != null) {
                var player = mc.world.getPlayerByUuid(clientPlayer);
                if (player == null)
                    return;

                if (player instanceof IMicrophoneHoldingClientPlayerEntity armPoseOverridable) {
                    armPoseOverridable.phonos$setHoldingState(WIRED);
                }

                var renderLayer = cableEndModel.getLayer(TEXTURE);
                var immediate = vertexConsumers.getBuffer(renderLayer);
                var config = PhonosClientConfig.get();
                var frustum = ((WorldRendererAccess) MinecraftClient.getInstance().worldRenderer).phonos$getFrustum();

                start.setOriginPose(
                    new Pose3f(
                        beCenter,
                        PhonosUtil.rotationTo(rotation)
                    )
                );
                Vec3d lerpedPos = player.getLeashPos(tickDelta);
                end.setPose(new Pose3f(
                    new Vector3f((float) lerpedPos.getX(), (float) lerpedPos.getY(), (float) lerpedPos.getZ()),
                    RotationAxis.POSITIVE_Y.rotationDegrees(-MathHelper.lerp(tickDelta, player.prevBodyYaw, player.bodyYaw) + 90)
                ));

                matrices.push();

                matrices.translate(-entity.getPos().getX(), -entity.getPos().getY(), -entity.getPos().getZ());

                CableRenderer.renderConnection(
                    null,
                    config,
                    entity.getWorld(),
                    conn,
                    boundCache,
                    frustum,
                    matrices,
                    immediate,
                    cableEndModel,
                    overlay,
                    tickDelta
                );

                matrices.pop();
            }
        }
    }

    public static void renderBakedItemModel(BakedModel model, int light, int overlay, MatrixStack matrixStack, VertexConsumer vc) {
        Random random = Random.create();
        for (Direction direction : Direction.values()) {
            random.setSeed(42L);
            renderBakedItemModelQuads(matrixStack, vc, model.getQuads(null, direction, random), light, overlay);
        }
        random.setSeed(42L);
        renderBakedItemModelQuads(matrixStack, vc, model.getQuads(null, null, random), light, overlay);
    }

    public static void renderBakedItemModelQuads(MatrixStack matrices, VertexConsumer vertices, Iterable<BakedQuad> quads, int light, int overlay) {
        MatrixStack.Entry entry = matrices.peek();
        for (BakedQuad bakedQuad : quads) {
            int color = -1;
            float r = (float)(color >> 16 & 0xFF) / 255.0f;
            float g = (float)(color >> 8 & 0xFF) / 255.0f;
            float b = (float)(color & 0xFF) / 255.0f;
            vertices.quad(entry, bakedQuad, r, g, b, light, overlay);
        }
    }

    @Override
    public boolean rendersOutsideBoundingBox(MicrophoneBaseBlockEntity blockEntity) {
        return super.rendersOutsideBoundingBox(blockEntity);
    }
}
