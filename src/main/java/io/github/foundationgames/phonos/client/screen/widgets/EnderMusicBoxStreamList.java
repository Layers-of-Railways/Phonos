package io.github.foundationgames.phonos.client.screen.widgets;

import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.widget.AlwaysSelectedEntryListWidget;
import net.minecraft.text.Text;
import org.lwjgl.glfw.GLFW;

public class EnderMusicBoxStreamList extends AlwaysSelectedEntryListWidget<EnderMusicBoxStreamList.Entry> {

    public EnderMusicBoxStreamList(MinecraftClient client, int width, int height, int top, int bottom, int itemHeight) {
        super(client, width, height, top, bottom, itemHeight);
    }

    public static class Entry extends AlwaysSelectedEntryListWidget.Entry<Entry> {
        private final EnderMusicBoxBlockEntity entity;
        public final long id;
        private final String name;
        private final TextRenderer textRenderer;

        private long ticks = 0;
        private long hoverStartTick = -1;

        public Entry(EnderMusicBoxBlockEntity entity, long id, String name, TextRenderer textRenderer) {
            super();

            this.entity = entity;
            this.id = id;
            this.name = name;
            this.textRenderer = textRenderer;
        }

        public void tick() {
            ticks++;
        }

        @Override
        public void drawBorder(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            super.drawBorder(context, index, y, x, entryWidth, entryHeight, mouseX, mouseY, hovered, tickDelta);
            context.drawBorder(x - 1, y - 1, entryWidth+2, entryHeight+2, -12303292);
        }

        @Override
        public void render(DrawContext context, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta) {
            context.fill(x, y, x + entryWidth, y + entryHeight, 0xFF909090);

            var name$ = Text.literal(name).asOrderedText();
            var width = textRenderer.getWidth(name$);
            context.drawTextWithShadow(textRenderer, name$, x + entryWidth - width - 4, y + 4, 0xFFFFFF);

            context.drawTextWithShadow(textRenderer, (index + 1)+".", x + 4, y + 4, 0xFFFFFF);

            if (hovered) {
                if (hoverStartTick == -1)
                    hoverStartTick = ticks;

                float progress = Math.min(((ticks - hoverStartTick) + tickDelta) / 5f, 1);
                progress = 0.1f + (progress * 0.9f);

                int brightness = (int) (progress * 255);
                int color = brightness << 16;

                context.drawTextWithShadow(textRenderer, "-".repeat(37), x, y + 5, color);
            } else {
                hoverStartTick = -1;
            }
        }

        @Override
        public boolean mouseClicked(double mouseX, double mouseY, int button) {
            if (button == GLFW.GLFW_MOUSE_BUTTON_LEFT) {
                ClientPayloadPackets.sendDeleteEnderMusicBoxStream(entity, id);

                return true;
            }

            return super.mouseClicked(mouseX, mouseY, button);
        }

        @Override
        public Text getNarration() {
            return Text.literal(name);
        }
    }
}
