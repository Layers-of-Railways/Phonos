package io.github.foundationgames.phonos.radio;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.util.math.BlockPos;

public record RadioMetadata(BlockPos pos, int transmissionRange) {

    public RadioMetadata(PacketByteBuf buf) {
        this(buf.readBlockPos(), buf.readVarInt());
    }

    public static void write(PacketByteBuf buf, RadioMetadata metadata) {
        metadata.write(buf);
    }

    public void write(PacketByteBuf buf) {
        buf.writeBlockPos(this.pos);
        buf.writeVarInt(this.transmissionRange);
    }

    public boolean shouldTransmitTo(RadioMetadata other) {
        return this.pos.isWithinDistance(other.pos, transmissionRange);
    }
}
