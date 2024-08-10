package io.github.foundationgames.phonos.client.render.entity;

import io.github.foundationgames.phonos.client.model.PhonosPartialModels;
import io.github.foundationgames.phonos.client.render.block.MicrophoneBaseBlockEntityRenderer;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingPlayerEntity;
import net.minecraft.client.model.ModelTransform;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.TexturedRenderLayers;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.entity.feature.FeatureRenderer;
import net.minecraft.client.render.entity.feature.FeatureRendererContext;
import net.minecraft.client.render.entity.model.EntityModelLoader;
import net.minecraft.client.render.entity.model.ModelWithArms;
import net.minecraft.client.render.entity.model.PlayerEntityModel;
import net.minecraft.client.render.model.BakedModel;
import net.minecraft.client.render.model.json.ModelTransformationMode;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Arm;
import net.minecraft.util.math.RotationAxis;
import org.joml.Quaternionf;

public class MicrophoneFeatureRenderer<E extends PlayerEntity, M extends PlayerEntityModel<E>> extends FeatureRenderer<E, M> {
    public MicrophoneFeatureRenderer(FeatureRendererContext<E, M> context, EntityModelLoader models) {
        super(context);
    }

    @Override
    public void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers, int light, E entity, float limbAngle, float limbDistance, float tickDelta, float animationProgress, float headYaw, float headPitch) {
        boolean leftHanded = entity.getMainArm() == Arm.LEFT;
        if (!(entity instanceof IMicrophoneHoldingPlayerEntity microphoneHolding) || !microphoneHolding.phonos$isHolding())
            return;

        BakedModel model = PhonosPartialModels.MICROPHONE.get();
        RenderLayer renderLayer = TexturedRenderLayers.getEntityTranslucentCull();

        /*{
            Matrix4f global = new Matrix4f();

            global.translate(0, 5, 0);
            global.rotate((float) Math.toRadians(145), new Vector3f(1, 1, 0).normalize());

            var pose = new Matrix4f(global);
            pose.translate(0, 4, 0);
            pose.rotate((float) Math.toRadians(45), new Vector3f(0, 1, 0));
            var invertedGlobal = new Matrix4f(global);
            invertedGlobal.invert();

            Vector3f test = new Vector3f(2, 0, 0);
            pose.transformPosition(test);
            invertedGlobal.transformPosition(test);
            Phonos.LOG.info("Position: {}", test);
        }*/

        {
            matrices.push();

            ((ModelWithArms) getContextModel()).setArmAngle(leftHanded ? Arm.LEFT : Arm.RIGHT, matrices);
            matrices.multiply(RotationAxis.POSITIVE_X.rotationDegrees(-90.0f));
            matrices.multiply(RotationAxis.POSITIVE_Y.rotationDegrees(180.0f));
            matrices.translate((float) (leftHanded ? -1 : 1) / 16.0f, 0.125f, -0.625f);

            {
                matrices.push();

                model.getTransformation().getTransformation(leftHanded ? ModelTransformationMode.THIRD_PERSON_LEFT_HAND : ModelTransformationMode.THIRD_PERSON_RIGHT_HAND).apply(leftHanded, matrices);
                matrices.translate(-0.5f, -0.5f, -0.5f);

                MicrophoneBaseBlockEntityRenderer.renderBakedItemModel(model, light,
                    OverlayTexture.DEFAULT_UV, matrices, vertexConsumers.getBuffer(renderLayer));

                matrices.pop();
            }

            matrices.pop();
        }

/*        matrices.push();
        MicrophoneBaseBlockEntityRenderer.renderBakedItemModel(model, light,
            OverlayTexture.DEFAULT_UV, matrices, vertexConsumers.getBuffer(renderLayer));
        matrices.pop();*/
    }

    private static void rotate(MatrixStack matrices, ModelTransform transform) {
        matrices.translate(transform.pivotX / 16.0f, transform.pivotY / 16.0f, transform.pivotZ / 16.0f);
        if (transform.pitch != 0.0f || transform.yaw != 0.0f || transform.roll != 0.0f) {
            matrices.multiply(new Quaternionf().rotationZYX(transform.roll, transform.yaw, transform.pitch));
        }
    }
}
