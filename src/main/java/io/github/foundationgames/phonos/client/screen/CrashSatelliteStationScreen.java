package io.github.foundationgames.phonos.client.screen;

import io.github.foundationgames.phonos.block.entity.SatelliteStationBlockEntity;
import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

public class CrashSatelliteStationScreen extends ConfirmScreen {
    public static final Text TITLE = Text.translatable("container.phonos.satellite_station");
    public static final Text MESSAGE = Text.translatable("message.phonos.crash_satellite_station").formatted(Formatting.RED);

    public CrashSatelliteStationScreen(SatelliteStationBlockEntity blockEntity) {
        super(ok -> {
            if (ok) {
                ClientPayloadPackets.sendRequestSatelliteAction(blockEntity, SatelliteStationBlockEntity.ACTION_CRASH, 0);
            }
            MinecraftClient.getInstance().setScreen(null);
        }, TITLE, MESSAGE);
    }
}
