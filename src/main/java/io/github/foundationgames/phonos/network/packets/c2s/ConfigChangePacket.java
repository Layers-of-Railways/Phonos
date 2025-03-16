package io.github.foundationgames.phonos.network.packets.c2s;

import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.ConfigField;
import dev.isxander.yacl3.config.v2.api.FieldAccess;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.config.serializers.NetworkConfigSerializer;
import io.github.foundationgames.phonos.network.packets.C2SPacket;
import io.github.foundationgames.phonos.util.PhonosUtil;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.math.MathHelper;

public record ConfigChangePacket(int idx, String name, PacketByteBuf serializedField) implements C2SPacket {
    public static final PacketCodec<PacketByteBuf, ConfigChangePacket> PACKET_CODEC = PacketCodec.tuple(
        PacketCodecs.VAR_INT,
        ConfigChangePacket::idx,
        PacketCodecs.string(PacketByteBuf.DEFAULT_MAX_STRING_LENGTH),
        ConfigChangePacket::name,
        PhonosUtil.NESTED_BUF_PACKET_CODEC,
        ConfigChangePacket::serializedField,
        ConfigChangePacket::new
    );

    public ConfigChangePacket(int i, FieldAccess<?> access) {
        this(i, access.name(), NetworkConfigSerializer.write(access));
    }

    @Override
    public void handle(ServerPlayerEntity sender) {
        if (!PhonosServerConfig.isAuthorizedToChange(sender)) {
            Phonos.LOG.warn("PLayer {} tried to change config without permission", sender);
            sender.networkHandler.disconnect(Text.of("You are not authorized to change Phonos config"));
            return;
        }

        ConfigClassHandler<PhonosServerConfig> config = PhonosServerConfig.getHandler(sender.getServerWorld());

        FieldAccess<?> access = config.fields()[MathHelper.clamp(idx, 0, config.fields().length-1)].access();

        if (access.name().equals(name)) {
            NetworkConfigSerializer.read(serializedField, access);
            config.save();
            return;
        }

        for (ConfigField<?> field : config.fields()) {
            access = field.access();
            if (access.name().equals(name)) {
                NetworkConfigSerializer.read(serializedField, access);
                config.save();
                return;
            }
        }

        Phonos.LOG.warn("Failed to find config field with name {}", name);
    }
}
