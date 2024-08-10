package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.MicrophoneBaseBlock;
import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.config.PhonosCommonConfig;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.util.compat.PhonosVoicechatPlugin;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import io.github.foundationgames.phonos.world.sound.block.OutputBlockEntity;
import io.github.foundationgames.phonos.world.sound.data.StreamSoundData;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.UUID;

public class MicrophoneBaseBlockEntity extends AbstractOutputBlockEntity implements Syncing, Ticking, OutputBlockEntity {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
        .addPoint(-3, -5, 0, Direction.WEST)
        .addPoint(3, -5, 0, Direction.EAST)
        .addPoint(0, -5, 3, Direction.SOUTH)
        .addPoint(0, -5, -3, Direction.NORTH);

    private final long streamId;
    private @Nullable SoundEmitterTree playingSound = null;

    private @Nullable WeakReference<ServerPlayerEntity> serverPlayer = null;
    private @Nullable UUID clientPlayer = null;

    public MicrophoneBaseBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state, OUTPUT_LAYOUT);

        this.streamId = UniqueId.obf(this.emitterId());
    }

    public MicrophoneBaseBlockEntity(BlockPos pos, BlockState state) {
        this(PhonosBlocks.MICROPHONE_BASE_ENTITY, pos, state);
    }

    public boolean isSpeakingPlayer(PlayerEntity player) {
        return this.serverPlayer != null && this.serverPlayer.get() == player;
    }

    @Override
    public Direction getRotation() {
        return getCachedState().get(MicrophoneBaseBlock.FACING);
    }

    public void start(ServerPlayerEntity serverPlayer) {
        if (world instanceof ServerWorld) {
            if (PhonosVoicechatPlugin.isStreaming(serverPlayer))
                return;

            this.playingSound = new SoundEmitterTree(this.emitterId);
            SoundStorage.getInstance(this.world).play(this.world,
                StreamSoundData.createMicrophone(this.emitterId(), this.streamId, SoundCategory.MASTER),
                this.playingSound);
            this.serverPlayer = new WeakReference<>(serverPlayer);

            if (PhonosVoicechatPlugin.startStream(serverPlayer, this.streamId)) {
                Phonos.LOG.info("Started microphone stream for player {} with stream ID {}", serverPlayer, this.streamId);
            } else {
                Phonos.LOG.warn("Failed to start microphone stream for player {} with stream ID {}", serverPlayer, this.streamId);
                this.stop();
            }
        }
        sync();
    }

    public void stop() {
        if (world instanceof ServerWorld && playingSound != null) {
            SoundStorage.getInstance(this.world).stop(this.world, this.emitterId());
            this.playingSound = null;

            PhonosVoicechatPlugin.stopStreaming(this.streamId);
            Phonos.LOG.info("Stopped microphone stream for player {} with stream ID {}", this.serverPlayer, this.streamId);
        }

        this.serverPlayer = null;
        sync();
    }

    public boolean isPlaying() {
        return this.playingSound != null;
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        super.tick(world, pos, state);

        if (world.isClient())
            return;

        if (this.playingSound != null) {
            var delta = this.playingSound.updateServer(world);

            if (delta.hasChanges() && world instanceof ServerWorld sWorld) for (var player : sWorld.getPlayers()) {
                PayloadPackets.sendSoundUpdate(player, delta);
            }
        } else {
            this.serverPlayer = null;
        }

        if (serverPlayer != null) {
            var player = serverPlayer.get();
            if (player == null || player.isRemoved() || !player.getMainHandStack().isEmpty() || !player.getPos().isInRange(getPos().toCenterPos(), PhonosCommonConfig.get().maxMicrophoneRange)) {
                this.stop();
            }
        }
    }

    @Override
    public boolean canConnect(ItemUsageContext ctx) {
        var side = ctx.getSide();
        if (side.getAxis().isHorizontal()) {
            var relPos = ctx.getHitPos().subtract(ctx.getBlockPos().toCenterPos());

            if (relPos.getY() < 0) {
                return !this.outputs.isOutputPluggedIn(OUTPUT_LAYOUT.getClosestIndexClicked(ctx.getHitPos(), this.getPos()));
            }
        }

        return false;
    }

    @Override
    public boolean addConnection(Vec3d hitPos, @Nullable DyeColor color, InputPlugPoint destInput, ItemStack cable) {
        int index = OUTPUT_LAYOUT.getClosestIndexClicked(hitPos, this.getPos());

        if (this.outputs.tryPlugOutputIn(index, color, destInput, cable)) {
            this.markDirty();
            this.sync();
            return true;
        }

        return false;
    }

    @Override
    public boolean forwards() {
        return false;
    }

    @Override
    protected void writeClientNbt(NbtCompound nbt) {
        super.writeClientNbt(nbt);
        if (this.serverPlayer != null) {
            ServerPlayerEntity player = this.serverPlayer.get();
            if (player != null) {
                nbt.putUuid("Player", player.getUuid());
            }
        }
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);
        if (nbt.containsUuid("Player")) {
            this.clientPlayer = nbt.getUuid("Player");
        } else {
            this.clientPlayer = null;
        }
    }

    public @Nullable UUID getClientPlayer() {
        return clientPlayer;
    }
}
