package io.github.foundationgames.phonos.network.packets;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.network.PhonosPackets;
import io.netty.buffer.ByteBuf;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.network.DisconnectionInfo;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.listener.ClientCommonPacketListener;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.network.packet.Packet;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class PacketSet {
    public final String id;
    public final int version;

    private final CustomPayload.Id<C2STunnel> c2sPacket;
    private final CustomPayload.Id<S2CTunnel> s2cPacket;

    private final List<PacketCodec<RegistryByteBuf, ? extends S2CPacket>> s2cPackets;
    private final Object2IntMap<Class<? extends S2CPacket>> s2cTypes;
    private final List<PacketCodec<RegistryByteBuf, ? extends C2SPacket>> c2sPackets;
    private final Object2IntMap<Class<? extends C2SPacket>> c2sTypes;

    public PacketSet(
        String id, int version,
        List<PacketCodec<RegistryByteBuf, ? extends S2CPacket>> s2cPackets,
        Object2IntMap<Class<? extends S2CPacket>> s2cTypes,
        List<PacketCodec<RegistryByteBuf, ? extends C2SPacket>> c2sPackets,
        Object2IntMap<Class<? extends C2SPacket>> c2sTypes
    ) {
        this.id = id;
        this.version = version;

        this.s2cPackets = s2cPackets;
        this.s2cTypes = s2cTypes;

        this.c2sPackets = c2sPackets;
        this.c2sTypes = c2sTypes;

        c2sPacket = new CustomPayload.Id<>(Identifier.of(id, "c2s"));
        s2cPacket = new CustomPayload.Id<>(Identifier.of(id, "s2c"));
    }

    /**
     * Send the given C2S packet to the server
     */
    @Environment(EnvType.CLIENT)
    public void send(C2SPacket packet) {
        ClientPlayNetworking.send(new C2STunnel(packet));
    }

    public void sendTo(ServerPlayerEntity player, S2CPacket packet) {
        ServerPlayNetworking.send(player, new S2CTunnel(packet));
    }

    public Packet<ClientCommonPacketListener> tunnelPacket(S2CPacket packet) {
        return ServerPlayNetworking.createS2CPacket(new S2CTunnel(packet));
    }

    private final PacketCodec<RegistryByteBuf, S2CTunnel> S2C_PACKET_CODEC = PacketCodec.ofStatic(this::writeS2CTunnel, this::readS2CTunnel);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void writeS2CTunnel(RegistryByteBuf buf, S2CTunnel tun) {
        int i = idOfS2C(tun.getPacket());
        if (i == -1) {
            throw new IllegalArgumentException("Cannot send unregistered S2CPacket: " + tun.getPacket());
        }
        buf.writeVarInt(i);
        ((PacketCodec) s2cPackets.get(i)).encode(buf, tun.getPacket());
    }
    private S2CTunnel readS2CTunnel(RegistryByteBuf buf) {
        int i = buf.readVarInt();
        if (i < 0 || i >= s2cPackets.size()) {
            Phonos.LOG.error("Invalid S2C Packet {}, ignoring", i);
            return new S2CTunnel(null);
        }
        return new S2CTunnel(s2cPackets.get(i).decode(buf));
    }
    private class S2CTunnel implements CustomPayload {
        private final S2CPacket packet;

        private S2CTunnel(S2CPacket packet) {
            this.packet = packet;
        }

        public S2CPacket getPacket() {
            return packet;
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return s2cPacket;
        }
    }

    private final PacketCodec<RegistryByteBuf, C2STunnel> C2S_PACKET_CODEC = PacketCodec.ofStatic(this::writeC2STunnel, this::readC2STunnel);
    @SuppressWarnings({"rawtypes", "unchecked"})
    private void writeC2STunnel(RegistryByteBuf buf, C2STunnel tun) {
        int i = idOfC2S(tun.getPacket());
        if (i == -1) {
            throw new IllegalArgumentException("Cannot send unregistered C2SPacket: " + tun.getPacket());
        }
        buf.writeVarInt(i);
        ((PacketCodec) c2sPackets.get(i)).encode(buf, tun.getPacket());
    }
    private C2STunnel readC2STunnel(RegistryByteBuf buf) {
        int i = buf.readVarInt();
        if (i < 0 || i >= c2sPackets.size()) {
            Phonos.LOG.error("Invalid C2S Packet {}, ignoring", i);
            return new C2STunnel(null);
        }
        return new C2STunnel(c2sPackets.get(i).decode(buf));
    }
    private class C2STunnel implements CustomPayload {
        private final C2SPacket packet;

        private C2STunnel(C2SPacket packet) {
            this.packet = packet;
        }

        public C2SPacket getPacket() {
            return packet;
        }

        @Override
        public Id<? extends CustomPayload> getId() {
            return c2sPacket;
        }
    }

    public void initCommon() {
        PayloadTypeRegistry.playS2C().register(s2cPacket, S2C_PACKET_CODEC);
        PayloadTypeRegistry.playS2C().register(c2sPacket, C2S_PACKET_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(c2sPacket, (payload, context) -> {
            ServerPlayerEntity sender = context.player();
            var packet = payload.getPacket();
            if (packet != null) {
                sender.server.execute(() -> packet.handle(sender));
            }
        });
    }

    @Environment(EnvType.CLIENT)
    public void initClient() {
        ClientPlayNetworking.registerGlobalReceiver(s2cPacket, (payload, context) -> {
            @SuppressWarnings("resource") MinecraftClient mc = context.client();
            var packet = payload.getPacket();
            if (packet != null) {
                mc.execute(() -> packet.handle(mc));
            }
        });
    }

    protected int idOfC2S(C2SPacket packet) {
        return c2sTypes.getOrDefault(packet.getClass(), -1);
    }

    protected int idOfS2C(S2CPacket packet) {
        return s2cTypes.getOrDefault(packet.getClass(), -1);
    }

    public static Builder builder(String id, int version) {
        return new Builder(id, version).s2c(CheckVersionPacket.class, CheckVersionPacket.PACKET_CODEC);
    }

    public record CheckVersionPacket(int serverVersion) implements S2CPacket {
        public static final PacketCodec<ByteBuf, CheckVersionPacket> PACKET_CODEC = PacketCodecs.VAR_INT.xmap(CheckVersionPacket::new, CheckVersionPacket::serverVersion);

        @Override
        @Environment(EnvType.CLIENT)
        public void handle(MinecraftClient mc) {
            if (PhonosPackets.PACKETS.version == serverVersion) return;
            Objects.requireNonNull(mc.getNetworkHandler()).onDisconnected(
                new DisconnectionInfo(Text.translatable("message.phonos.network.version_mismatch"))
            );
        }
    }

    public static class Builder {
        public final String id;
        public final int version;

        private final List<PacketCodec<RegistryByteBuf, ? extends S2CPacket>> s2cPackets = new ArrayList<>();
        private final Object2IntMap<Class<? extends S2CPacket>> s2cTypes = new Object2IntOpenHashMap<>();
        private final List<PacketCodec<RegistryByteBuf, ? extends C2SPacket>> c2sPackets = new ArrayList<>();
        private final Object2IntMap<Class<? extends C2SPacket>> c2sTypes = new Object2IntOpenHashMap<>();

        protected Builder(String id, int version) {
            this.id = id;
            this.version = version;
        }

        public <P extends S2CPacket> Builder s2c(Class<P> clazz, PacketCodec<? super RegistryByteBuf, P> codec) {
            s2cPackets.add(codec.cast());
            s2cTypes.put(clazz, s2cPackets.indexOf(codec));
            return this;
        }

        public <P extends C2SPacket> Builder c2s(Class<P> clazz, PacketCodec<? super RegistryByteBuf, P> codec) {
            c2sPackets.add(codec.cast());
            c2sTypes.put(clazz, c2sPackets.indexOf(codec));
            return this;
        }

        public PacketSet build() {
            return new PacketSet(id, version, s2cPackets, s2cTypes, c2sPackets, c2sTypes);
        }
    }
}
