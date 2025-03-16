package io.github.foundationgames.phonos.network.packets.c2s;

import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.netty.buffer.ByteBuf;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.ItemStack;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.ClickType;

public record FakeCreativeSlotClickPacket(ItemStack onto, ItemStack with, ClickType click) implements C2SPacket {
    private static final PacketCodec<ByteBuf, ClickType> CLICK_TYPE_CODEC = PacketCodecs.VAR_INT.xmap(
        i -> ClickType.values()[i],
        ClickType::ordinal
    );

    public static final PacketCodec<RegistryByteBuf, FakeCreativeSlotClickPacket> PACKET_CODEC = PacketCodec.tuple(
        ItemStack.OPTIONAL_PACKET_CODEC,
        FakeCreativeSlotClickPacket::onto,
        ItemStack.OPTIONAL_PACKET_CODEC,
        FakeCreativeSlotClickPacket::with,
        CLICK_TYPE_CODEC,
        FakeCreativeSlotClickPacket::click,
        FakeCreativeSlotClickPacket::new
    );

    @Override
    public void handle(ServerPlayerEntity sender) {
        onto.getItem().onClicked(onto, with, null, click, sender, StackReference.EMPTY);
    }
}
