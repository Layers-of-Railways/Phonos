package io.github.foundationgames.phonos.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class PhonosConfigHomeScreen extends Screen {
    private static final Text TITLE = Text.translatable("text.config.phonos.title");
    private static final Text CLIENT_TITLE = Text.translatable("text.config.phonos.client_title");
    private static final Text COMMON_TITLE = Text.translatable("text.config.phonos.common_title");

    private final Screen parent;

    public PhonosConfigHomeScreen(Screen parent) {
        super(TITLE);
        this.parent = parent;
    }

    @Override
    public void close() {
        this.client.setScreen(this.parent);
    }

    @Override
    protected void init() {
        var bg = new OptionListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);
        this.addDrawable(bg);

        this.addDrawableChild(ButtonWidget.builder(CLIENT_TITLE, (button) -> this.client.setScreen(PhonosConfigScreen.create(PhonosClientConfig.get(), this)))
            .dimensions(this.width / 2 - 75, 40, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(COMMON_TITLE, (button) -> this.client.setScreen(PhonosConfigScreen.create(PhonosCommonConfig.get(), this)))
            .dimensions(this.width / 2 - 75, 68, 150, 20).build());

        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.close())
            .dimensions(this.width / 2 - 75, this.height - 27, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 13, 0xFFFFFF);
    }
}
