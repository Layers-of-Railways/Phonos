package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.block.SatelliteReceiverBlock;
import io.github.foundationgames.phonos.satellite_radio.SatelliteRadioDevice;
import io.github.foundationgames.phonos.satellite_radio.SatelliteRadioStorage;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.RadarPoints;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import net.minecraft.block.BlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class SatelliteReceiverBlockEntity extends AbstractConnectionHubBlockEntity implements SatelliteRadioDevice.Receiver {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
            .addPoint(-8, -5, 0, Direction.WEST)
            .addPoint(8, -5, 0, Direction.EAST);

    private String channel = "";
    private boolean needsAdd = false;
    private boolean unmigrated = false;

    public SatelliteReceiverBlockEntity(BlockPos pos, BlockState state) {
        super(PhonosBlocks.SATELLITE_RECEIVER_ENTITY, pos, state, OUTPUT_LAYOUT, new boolean[0]);
    }

    @Override
    protected void readNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.readNbt(nbt, registryLookup);

        String channel;
        if (nbt.contains("channel", NbtElement.INT_TYPE)) {
            unmigrated = true;
            channel = "N" + nbt.getInt("channel");
        } else {
            channel = nbt.getString("channel");
            unmigrated = nbt.getBoolean("unmigrated");
        }

        if (unmigrated) {
            String migrated = SatelliteRadioStorage.convertLegacyChannel(channel);
            if (migrated != null) {
                channel = migrated;
                unmigrated = false;
            }
        }

        if (this.world == null) {
            this.needsAdd = true;
            this.channel = channel;
        } else {
            var um = unmigrated;
            this.setAndUpdateChannel(channel);
            unmigrated = um;
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt, RegistryWrapper.WrapperLookup registryLookup) {
        super.writeNbt(nbt, registryLookup);

        nbt.putString("channel", this.getChannel());
        if (unmigrated) {
            nbt.putBoolean("unmigrated", true);
        }
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();

        if (world instanceof ServerWorld sWorld) {
            RadarPoints.get(sWorld).remove(this.channel, this.pos);
        }
    }

    @Override
    public String getChannel() {
        return channel;
    }

    @Override
    public boolean canConnect(ItemUsageContext ctx) {
        var side = ctx.getSide();
        var facing = getRotation();

        if (side.getAxis().isHorizontal() && side.getAxis() != facing.getAxis()) {
            return !this.outputs.isOutputPluggedIn(OUTPUT_LAYOUT.getClosestIndexClicked(ctx.getHitPos(), this.getPos(), facing));
        }

        return false;
    }

    @Override
    public boolean addConnection(Vec3d hitPos, @Nullable DyeColor color, InputPlugPoint destInput, ItemStack cable) {
        int index = OUTPUT_LAYOUT.getClosestIndexClicked(hitPos, this.getPos(), getRotation());

        if (this.outputs.tryPlugOutputIn(index, color, destInput, cable)) {
            this.markDirty();
            this.sync();
            return true;
        }

        return false;
    }

    @Override
    public boolean forwards() {
        if (world != null && SatelliteStationBlockEntity.validateChannel(getChannel())) {
            SatelliteRadioStorage.getInstance(world).keepAlive(getChannel());
        }
        return true;
    }

    @Override
    public Direction getRotation() {
        if (this.getCachedState().getBlock() instanceof SatelliteReceiverBlock block) {
            return block.getRotation(this.getCachedState());
        }

        return Direction.NORTH;
    }

    @Override
    public void setAndUpdateChannel(String channel) {
        channel = SatelliteStationBlockEntity.cleanChannel(channel);
        if (!SatelliteStationBlockEntity.validateChannel(channel)) return;

        if (!channel.equals(this.channel)) {
            unmigrated = false;
        }

        if (this.world instanceof ServerWorld sWorld) for (boolean in : this.inputs) if (in) {
            RadarPoints.get(sWorld).remove(getChannel(), this.pos);
            RadarPoints.get(sWorld).add(channel, this.pos);
            break;
        }

        var satelliteStorage = SatelliteRadioStorage.getInstance(this.world);

        satelliteStorage.removeReceivingEmitter(getChannel(), this.emitterId());
        this.channel = channel;
        if (!getChannel().isBlank()) {
            satelliteStorage.addReceivingEmitter(getChannel(), this);
        }

        sync();
    }

    @Override
    public void addReceiver() {
        setAndUpdateChannel(getChannel());
    }

    @Override
    public void removeReceiver() {
        var satelliteStorage = SatelliteRadioStorage.getInstance(this.world);
        satelliteStorage.removeReceivingEmitter(this.getChannel(), this.emitterId());
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        super.tick(world, pos, state);

        if (this.needsAdd) {
            this.addReceiver();
            this.needsAdd = false;
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {(%s) %s channel '%s'}".formatted(
            getPos().toShortString(),
            UniqueId.debugNameOf(emitterId()),
            getChannel()
        );
    }
}
