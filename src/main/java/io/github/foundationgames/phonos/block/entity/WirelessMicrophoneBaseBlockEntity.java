package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.item.PhonosItems;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import net.minecraft.block.BlockState;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.math.BlockPos;

public class WirelessMicrophoneBaseBlockEntity extends MicrophoneBaseBlockEntity {
    public WirelessMicrophoneBaseBlockEntity(BlockPos pos, BlockState state) {
        super(PhonosBlocks.WIRELESS_MICROPHONE_BASE_ENTITY, pos, state);
    }

    @Override
    protected int getRange() {
        return PhonosServerConfig.get(world).maxWirelessMicrophoneRange;
    }

    @Override
    public boolean canStart(ServerPlayerEntity serverPlayer) {
        // gets rid of empty-hand condition and adds headset condition
        return serverPlayer.getPos().isInRange(getPos().toCenterPos(), getRange()) &&
            !PhonosVoicechatProxy.isStreaming(serverPlayer) &&
            serverPlayer.getEquippedStack(EquipmentSlot.HEAD).getItem() == PhonosItems.HEADSET;
    }
}
