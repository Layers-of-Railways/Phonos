package io.github.foundationgames.phonos.client.render;

import com.google.common.hash.Hashing;
import io.github.foundationgames.phonos.block.entity.RadioTransceiverBlockEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;

@Environment(EnvType.CLIENT)
public class RadioDebugRenderer {
    private RadioDebugRenderer() {}

    private static final Map<BlockPos, DebugMeta> targets = new HashMap<>();

    public static void addTarget(BlockPos pos) {
        var world = MinecraftClient.getInstance().world;
        if (world == null)
            return;
        if (!(world.getBlockEntity(pos) instanceof RadioTransceiverBlockEntity be))
            return;

        targets.put(pos, new DebugMeta(be.getMetadata().transmissionRange(), colorFromLong(be.emitterId())));
    }

    public static void removeTarget(BlockPos pos) {
        targets.remove(pos);
    }

    public static void clearTargets() {
        targets.clear();
    }

    public static void tick(@NotNull World world) {
        var iterator = targets.entrySet().iterator();

        while (iterator.hasNext()) {
            var entry = iterator.next();
            var pos = entry.getKey();
            if (world.getBlockEntity(pos) instanceof RadioTransceiverBlockEntity be) {
                entry.setValue(new DebugMeta(be.getMetadata().transmissionRange(), colorFromLong(be.emitterId())));
            } else {
                iterator.remove();
            }
        }
    }

    public static void render(MatrixStack matrices, VertexConsumerProvider vertexConsumers) {
        var cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        matrices.push();
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        for (var entry : targets.entrySet()) {
            var pos = entry.getKey();
            var meta = entry.getValue();

            var range = meta.range;
            var color = meta.color;

            var x = pos.getX();
            var y = pos.getY();
            var z = pos.getZ();

            var r = ((color >> 16) & 0xFF) / 255.0f;
            var g = ((color >> 8) & 0xFF) / 255.0f;
            var b = (color & 0xFF) / 255.0f;

            matrices.push();

            matrices.translate(x + 0.5, y + 0.5, z + 0.5);

            int horizontalPoints = 64;
            int verticalPoints = 32;

            // vertical lines
            for (int horiz = 0; horiz < horizontalPoints; horiz++) {
                var vc = vertexConsumers.getBuffer(RenderLayer.getLineStrip());
                for (int vert = -verticalPoints; vert < verticalPoints; vert++) {
                    double x1 = Math.cos(horiz * 2 * Math.PI / horizontalPoints) * Math.sin(vert * Math.PI / verticalPoints);
                    double y1 = Math.cos(vert * Math.PI / verticalPoints);
                    double z1 = Math.sin(horiz * 2 * Math.PI / horizontalPoints) * Math.sin(vert * Math.PI / verticalPoints);

                    var matrix4f = matrices.peek().getPositionMatrix();
                    var matrix3f = matrices.peek().getNormalMatrix();

                    vc.vertex(matrix4f, (float) (x1*range), (float) (y1*range), (float) (z1*range)).color(r, g, b, 1.0f).normal(matrix3f, 0, 1, 0).next();
                }
            }

            // horizontal lines
            for (int vert = -verticalPoints; vert < verticalPoints; vert++) {
                var vc = vertexConsumers.getBuffer(RenderLayer.getLineStrip());
                for (int horiz = 0; horiz < horizontalPoints; horiz++) {
                    int actualVert = vert;
                    if (vert == -verticalPoints) actualVert = -verticalPoints + 1;

                    boolean special = actualVert != vert;

                    double x1 = Math.cos(horiz * 2 * Math.PI / horizontalPoints) * Math.sin(actualVert * Math.PI / verticalPoints);
                    double y1 = special ? 0 : Math.cos(actualVert * Math.PI / verticalPoints);
                    double z1 = Math.sin(horiz * 2 * Math.PI / horizontalPoints) * Math.sin(actualVert * Math.PI / verticalPoints);

                    var matrix4f = matrices.peek().getPositionMatrix();
                    var matrix3f = matrices.peek().getNormalMatrix();

                    float horizRange = special ? 6 : range;

                    vc.vertex(matrix4f, (float) (x1*horizRange), (float) (y1*range), (float) (z1*horizRange)).color(r, g, b, 1.0f).normal(matrix3f, 0, 1, 0).next();
                }
            }

            matrices.pop();
        }

        matrices.pop();
    }

    public record DebugMeta(int range, int color) {}

    private static int rainbowColor(int timeStep) {
        int localTimeStep = Math.abs(timeStep) % 1536;
        int timeStepInPhase = localTimeStep % 256;
        int phaseBlue = localTimeStep / 256;
        int red = colorInPhase(phaseBlue + 4, timeStepInPhase);
        int green = colorInPhase(phaseBlue + 2, timeStepInPhase);
        int blue = colorInPhase(phaseBlue, timeStepInPhase);

        return 0xff << 24 | red << 16 | green << 8 | blue;
    }

    private static int colorInPhase(int phase, int progress) {
        phase = phase % 6;
        if (phase <= 1)
            return 0;
        if (phase == 2)
            return progress;
        if (phase <= 4)
            return 255;
        else
            return 255 - progress;
    }

    private static int colorFromLong(long l) {
        return rainbowColor(Hashing.crc32().hashLong(l).asInt());
    }
}
