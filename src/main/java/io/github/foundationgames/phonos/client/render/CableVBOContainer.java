package io.github.foundationgames.phonos.client.render;

import com.mojang.blaze3d.systems.RenderSystem;
import io.github.foundationgames.phonos.client.model.BasicModel;
import io.github.foundationgames.phonos.config.PhonosClientConfig;
import io.github.foundationgames.phonos.world.sound.CableConnection;
import io.github.foundationgames.phonos.world.sound.ConnectionCollection;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.VertexBuffer;
import net.minecraft.client.render.*;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;

public class CableVBOContainer {
    public boolean rebuild;
    public @Nullable VertexBuffer buffer = null;
    private List<CableConnection> cachedCons = new ArrayList<>();

    private CableBounds bounds = new CableBounds();
    BufferBuilder wipBuilder = null;

    public void refresh(ConnectionCollection conns) {
        List<CableConnection> connections = new ArrayList<>();
        conns.forEach((i, conn) -> {
            if (conn.isStatic()) {
                connections.add(conn);
            }
        });

        if (!cachedCons.equals(connections) || this.buffer == null) {
            // Mark ourselves as needing re-rendering
            rebuild = true;

            // Close the existing buffer
            if (buffer != null) {
                buffer.close();
            }

            buffer = null;
            cachedCons = connections;
        }
    }

    public void render(MatrixStack matrices, VertexConsumer immediate, RenderLayer layer, BasicModel cableEndModel, Frustum frustum,
                       ConnectionCollection conns, PhonosClientConfig config, World world, int overlay, float tickDelta) {
        if (conns.getOutputCount() == 0) return;

        boolean rebuild = this.buffer == null || this.rebuild;

        BufferBuilder builder;
        if (rebuild) {
            // If we're re-rendering into the vertexbuffer, create a new VBO,
            // grab the tessellator and start tessellating with our vertex format
            var vbo = new VertexBuffer(VertexBuffer.Usage.STATIC);
            builder = Tessellator.getInstance().begin(VertexFormat.DrawMode.QUADS, VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL);
            wipBuilder = builder;

            this.buffer = vbo;

            this.bounds.clear();
        } else {
            builder = null; // because Java can't infer that `builder` is initialized in the next `if (rebuild)` block
        }

        // Render each connection point in immediate mode, and render cables into the given vertex buffer
        conns.forEach((i, conn) ->
                CableRenderer.renderConnection(this, config, world, conn, rebuild ? bounds : null, frustum,
                        matrices, immediate, cableEndModel, overlay, tickDelta));

        var vbo = this.buffer;

        if (rebuild) {
            // If we rerendered, upload the buffer to the GPU and mark ourselves as not dirty
            vbo.bind();
            vbo.upload(builder.end());
            VertexBuffer.unbind();
            this.rebuild = false;
        }

        wipBuilder = null;

        if (config.cableCulling && !bounds.visible(frustum)) {
            return;
        }

        // Set up the render state for this render phase (and texture)
        layer.startDrawing();

        // Grab fog and set to an extravagant value
        // TODO: this is a total hack, but it's needed to convince the shader to not apply fog everywhere
        float realEnd = RenderSystem.getShaderFogEnd();
        RenderSystem.setShaderFogEnd(9999999);

        matrices.push();

        var cameraPos = MinecraftClient.getInstance().gameRenderer.getCamera().getPos();
        matrices.translate(cameraPos.x, cameraPos.y, cameraPos.z);
        matrices.multiplyPositionMatrix(RenderSystem.getModelViewMatrix());
        matrices.translate(-cameraPos.x, -cameraPos.y, -cameraPos.z);

        // Render the buffer, which contains all the cables connected to this
        vbo.bind();

        vbo.draw(matrices.peek().getPositionMatrix(), RenderSystem.getProjectionMatrix(), GameRenderer.getRenderTypeEntitySolidProgram());
        VertexBuffer.unbind();

        matrices.pop();

        // Reset the render state
        RenderSystem.setShaderFogEnd(realEnd);
        layer.endDrawing();
    }

    public void close() {
        if (buffer != null) {
            buffer.close();
        }

        buffer = null;
    }
}
