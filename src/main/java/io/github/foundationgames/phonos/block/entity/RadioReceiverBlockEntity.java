package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.RadioReceiverBlock;
import io.github.foundationgames.phonos.radio.RadioDevice;
import io.github.foundationgames.phonos.radio.RadioMetadata;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.RadarPoints;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.BlockConnectionLayout;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.DyeColor;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

public abstract class RadioReceiverBlockEntity extends AbstractConnectionHubBlockEntity implements RadioDevice.Receiver {
    public static final BlockConnectionLayout OUTPUT_LAYOUT = new BlockConnectionLayout()
            .addPoint(-8, -5, 0, Direction.WEST)
            .addPoint(8, -5, 0, Direction.EAST);

    private int channel = 0;
    private boolean needsAdd = false;

    public RadioReceiverBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state, boolean[] inputs) {
        super(type, pos, state, OUTPUT_LAYOUT, inputs);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        if (this.world == null) {
            this.needsAdd = true;
            this.channel = nbt.getInt("channel");
        } else {
            this.setAndUpdateChannel(nbt.getInt("channel"));
        }
    }

    @Override
    protected void writeNbt(NbtCompound nbt) {
        super.writeNbt(nbt);

        nbt.putInt("channel", this.getChannelRaw());
    }

    @Override
    public void onDestroyed() {
        super.onDestroyed();

        if (world instanceof ServerWorld sWorld) {
            RadarPoints.get(sWorld).remove(this.channel, this.pos);
        }
    }

    private int transformChannel(int channel) {
        return isSatellite() ? RadioStorage.toSatelliteBand(channel) : channel;
    }

    @ApiStatus.Internal
    public int getChannelRaw() {
        return channel;
    }

    @Override
    public int getChannel() {
        return transformChannel(channel);
    }

    @Override
    public RadioMetadata getMetadata() {
        return new RadioMetadata(getPos(), 0);
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
        return true;
    }

    @Override
    public Direction getRotation() {
        if (this.getCachedState().getBlock() instanceof RadioReceiverBlock block) {
            return block.getRotation(this.getCachedState());
        }

        return Direction.NORTH;
    }

    public int getChannelCount() {
        return isSatellite() ? RadioStorage.SATELLITE_CHANNEL_COUNT : RadioStorage.RADIO_CHANNEL_COUNT;
    }

    @Override
    public void setAndUpdateChannel(int channel) {
        channel = Math.floorMod(channel, getChannelCount());

        if (this.world instanceof ServerWorld sWorld) for (boolean in : this.inputs) if (in) {
            RadarPoints.get(sWorld).remove(getChannel(), this.pos);
            RadarPoints.get(sWorld).add(transformChannel(channel), this.pos);
            break;
        }

        var radio = RadioStorage.getInstance(this.world);

        radio.removeReceivingEmitter(getChannel(), this.emitterId());
        this.channel = channel;
        radio.addReceivingEmitter(getChannel(), this);

        sync();
    }

    @Override
    public void addReceiver() {
        setAndUpdateChannel(getChannelRaw());
    }

    @Override
    public void removeReceiver() {
        var radio = RadioStorage.getInstance(this.world);
        radio.removeReceivingEmitter(this.getChannel(), this.emitterId());
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        super.tick(world, pos, state);

        if (this.needsAdd) {
            this.addReceiver();
            this.needsAdd = false;
        }
    }

    public int getTransmissionTowerHeight() {
        return 0;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {(%s) %s%s channel#%s}".formatted(
            getPos().toShortString(),
            UniqueId.debugNameOf(emitterId()),
            isSatellite() ? " satellite" : "",
            getChannelRaw()
        );
    }

    public abstract boolean isSatellite();
}
