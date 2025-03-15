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

public class LaunchSatelliteStationScreen extends Screen {
    public static final Text TITLE = Text.translatable("container.phonos.satellite_station");
    public static final Text SELECT = Text.translatable("message.phonos.launch_satellite_station.select");
    public static final Text LAUNCHING = Text.translatable("message.phonos.satellite_launching");
    public static final Text LAUNCH = Text.translatable("message.phonos.launch_satellite_station.launch");

    protected final SatelliteStationBlockEntity station;

    private TextFieldWidget channelField;
    private ButtonWidget launchButton;

    private boolean launched;

    public LaunchSatelliteStationScreen(SatelliteStationBlockEntity entity) {
        super(TITLE);

        this.station = entity;
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

        this.launchButton = this.addDrawableChild(ButtonWidget.builder(LAUNCH, b -> this.launch())
            .position(this.width / 2 - 80, 180)
            .size(160, 20)
            .build());

        this.launchButton.active = this.validateChannel() == null;
        this.channelField.active = true;

        this.setInitialFocus(this.channelField);
    }

    @Override
    public void render(DrawContext context, int mouseX, int mouseY, float delta) {
        super.render(context, mouseX, mouseY, delta);

        context.drawCenteredTextWithShadow(this.textRenderer, this.title, this.width / 2, 80, 0xFFFFFF);
        context.drawCenteredTextWithShadow(this.textRenderer, launched ? LAUNCHING : SELECT, this.width  / 2, 100, 0xDDDDDD);

        if (!launched) {
            var validationMessage = this.validateChannel();
            if (validationMessage != null) {
                context.drawCenteredTextWithShadow(this.textRenderer, validationMessage, this.width / 2, 120, 0xDDDDDD);
            }
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.launchButton.active = this.validateChannel() == null && !launched;
        this.launchButton.visible = !launched;

        this.channelField.active = this.channelField.visible = !launched;
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

    private void launch() {
        if (this.validateChannel() != null)
            return;

        var channel = this.channelField.getText();
        ClientPayloadPackets.sendRequestSatelliteAction(this.station, SatelliteStationBlockEntity.ACTION_LAUNCH, channel);

        launchButton.active = false;
        launchButton.visible = false;
        channelField.active = false;
        channelField.visible = false;

        launched = true;
    }
}
