package io.github.foundationgames.phonos.network.packets.s2c;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.config.serializers.NetworkConfigSerializer;
import io.github.foundationgames.phonos.network.packets.S2CPacket;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;

public record SetConfigPacket(PacketByteBuf serializedConfig) implements S2CPacket {
    public static final PacketCodec<PacketByteBuf, SetConfigPacket> PACKET_CODEC = PhonosUtil.NESTED_BUF_PACKET_CODEC.xmap(
        SetConfigPacket::new,
        SetConfigPacket::serializedConfig
    );

    public SetConfigPacket(ConfigClassHandler<PhonosServerConfig> config) {
        this(NetworkConfigSerializer.write(config));
    }

    @Override
    @Environment(EnvType.CLIENT)
    public void handle(MinecraftClient mc) {
        var config = PhonosServerConfig.getHandler(mc.world);
        NetworkConfigSerializer.read(serializedConfig, config);
    }
}
