package io.github.foundationgames.phonos.sound;

import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.nbs.stream.ClientIncomingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.nbs.stream.ServerOutgoingNBSStreamHandler;
import io.github.foundationgames.phonos.sound.stream.ClientIncomingStreamHandler;
import io.github.foundationgames.phonos.sound.stream.ServerOutgoingStreamHandler;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatProxy;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.server.network.ServerPlayerEntity;
import org.jetbrains.annotations.ApiStatus;

public class ResyncManager {
    @Environment(EnvType.CLIENT)
    public static void prepareClient() {
        ClientIncomingStreamHandler.reset();
        ClientIncomingNBSStreamHandler.reset();
        PhonosVoicechatProxy.cleanupOnDisconnect();

        SoundStorage.stopAllClient();
        SoundStorage.clientReset();
    }

    public static void prepareServer(ServerPlayerEntity player) {
        ServerOutgoingStreamHandler.prepareForResync(player);
        ServerOutgoingNBSStreamHandler.prepareForResync(player);

        PayloadPackets.sendPrepareForResync(player);
    }

    @ApiStatus.Internal
    public static void resyncServer(ServerPlayerEntity player) {
        SoundStorage.getInstance(player.getServerWorld())
            .registerPlayerWaitingForResume(player);
    }
}
