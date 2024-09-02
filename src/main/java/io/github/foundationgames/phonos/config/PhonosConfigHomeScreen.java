package io.github.foundationgames.phonos.config;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.tooltip.Tooltip;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.OptionListWidget;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;

public class PhonosConfigHomeScreen extends Screen {
    private static final Text TITLE = Text.translatable("text.config.phonos.title");
    private static final Text CLIENT_TITLE = Text.translatable("text.config.phonos.client_title");
    private static final Text SERVER_TITLE = Text.translatable("text.config.phonos.server_title");
    private static final Text SERVER_TITLE_DISABLED_TOOLTIP = Text.translatable("text.config.phonos.server_title.disabled_tooltip");

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
        assert this.client != null;

        var bg = new OptionListWidget(this.client, this.width, this.height, 32, this.height - 32, 25);
        this.addDrawable(bg);

        this.addDrawableChild(ButtonWidget.builder(CLIENT_TITLE, (button) -> this.client.setScreen(PhonosClientConfig.createScreen(this)))
            .dimensions(this.width / 2 - 75, 40, 150, 20).build());


        ButtonWidget serverConfigButton = ButtonWidget.builder(SERVER_TITLE, (button) -> {
                if (this.client.world != null) {
                    this.client.setScreen(PhonosServerConfig.createScreen(this));
                }
            })
            .dimensions(this.width / 2 - 75, 68, 150, 20).build();

        if (this.client.world == null) {
            serverConfigButton.active = false;
            serverConfigButton.setTooltip(Tooltip.of(SERVER_TITLE_DISABLED_TOOLTIP));
        }

        this.addDrawableChild(serverConfigButton);


        this.addDrawableChild(ButtonWidget.builder(ScreenTexts.DONE, (button) -> this.close())
            .dimensions(this.width / 2 - 75, this.height - 27, 150, 20).build());
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 13, 0xFFFFFF);
    }
}
