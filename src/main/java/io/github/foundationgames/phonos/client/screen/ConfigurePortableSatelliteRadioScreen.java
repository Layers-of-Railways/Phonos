package io.github.foundationgames.phonos.client.screen;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import org.apache.commons.lang3.mutable.MutableObject;
import org.jetbrains.annotations.Nullable;

public class ConfigurePortableSatelliteRadioScreen extends Screen {
    public static final Text TITLE = Text.translatable("item.phonos.portable_satellite_radio");
    public static final Text SELECT = Text.translatable("message.phonos.portable_satellite_radio.select");
    public static final Text CONFIGURE = Text.translatable("message.phonos.portable_satellite_radio.configure");

    private final int toolbarSlot;

    private TextFieldWidget channelField;
    private ButtonWidget configureButton;

    public ConfigurePortableSatelliteRadioScreen(int toolbarSlot) {
        super(TITLE);

        this.toolbarSlot = toolbarSlot;
    }

    @Override
    public boolean shouldPause() {
        return false;
    }

    @Override
    protected void init() {
        super.init();

        this.channelField = this.addDrawableChild(new TextFieldWidget(this.textRenderer, this.width / 2 - 80, 150, 160, 20, Text.of("")));

        this.channelField.setChangedListener(text -> {
            String newText = SatelliteStationBlockEntity.cleanChannel(text);
            if (!text.equals(newText)) {
                this.channelField.setText(newText);
            }
        });

        this.configureButton = this.addDrawableChild(ButtonWidget.builder(CONFIGURE, b -> this.configure())
            .position(this.width / 2 - 80, 180)
            .size(160, 20)
            .build());

        this.configureButton.active = this.validateChannel() == null;
        this.channelField.active = true;

        this.setInitialFocus(this.channelField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        this.renderBackground(context);

        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 80, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, SELECT, this.width  / 2, 100, 0xDDDDDD);

        var validationMessage = this.validateChannel();
        if (validationMessage != null) {
            context.drawCenteredTextWithShadow(this.textRenderer, validationMessage, this.width / 2, 120, 0xDDDDDD);
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.configureButton.active = this.validateChannel() == null;
        this.configureButton.visible = true;

        this.channelField.active = this.channelField.visible = true;

        if (client == null || client.player == null || client.player.getInventory().selectedSlot != this.toolbarSlot) {
            this.close();
        }
    }

    private @Nullable Text validateChannel() {
        var text = this.channelField.getText();
        if (text.isEmpty()) {
            return Text.translatable("message.phonos.launch_satellite_station.no_channel")
                .formatted(Formatting.RED);
        }

        MutableObject<String> reason = new MutableObject<>();
        if (!SatelliteStationBlockEntity.validateChannel(text, reason)) {
            MutableText message;
            if (reason.getValue() != null) {
                message = Text.translatable("message.phonos.launch_satellite_station.invalid_channel."+reason.getValue());
            } else {
                message = Text.translatable("message.phonos.launch_satellite_station.invalid_channel");
            }
            return message.formatted(Formatting.RED);
        }

        return null;
    }

    private void configure() {
        if (this.validateChannel() != null)
            return;

        if (client == null || client.player == null)
            return;

        if (client.player.getInventory().selectedSlot != this.toolbarSlot)
            return;

        ClientPayloadPackets.sendConfigurePortableSatelliteRadioChannel(this.channelField.getText());

        close();
    }
}
