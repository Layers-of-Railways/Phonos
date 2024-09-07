package io.github.foundationgames.phonos.block.entity;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.item.PhonosItems;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.sound.emitter.ForwardingSoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundSource;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.RadarPoints;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.entity.ItemEntity;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.listener.ClientPlayPacketListener;
import net.minecraft.network.packet.Packet;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Vector3d;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

public class SatelliteStationBlockEntity extends BlockEntity implements Syncing, Ticking, ForwardingSoundEmitter {
    public static final int ACTION_CRASH = 0;
    public static final int ACTION_LAUNCH = 1;

    public static final int SCREEN_CRASH = 0;
    public static final int SCREEN_LAUNCH = 1;

    private Vec3d launchpadPos = null;
    private Status status = Status.NONE;

    private Rocket rocket = null;

    private int channel = 0;
    private boolean needsAdd = false;

    public final boolean[] inputs = new boolean[2];

    protected final long emitterId;

    public SatelliteStationBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
        this.emitterId = UniqueId.ofBlock(getPos());
    }

    public SatelliteStationBlockEntity(BlockPos pos, BlockState state) {
        this(PhonosBlocks.SATELLITE_STATION_ENTITY, pos, state);
    }

    public boolean addRocket() {
        if (this.rocket != null || this.status != Status.NONE) {
            return false;
        }

        this.rocket = dormantRocket();
        sync();
        markDirty();

        return true;
    }

    @Override
    public void tick(World world, BlockPos pos, BlockState state) {
        if (this.needsAdd) {
            this.setAndUpdateChannel(this.getChannel());
            this.needsAdd = false;
        }

        if (this.rocket != null) {
            this.rocket.tick();

            if (this.rocket.removed) {
                this.rocket = null;
                this.status = Status.IN_ORBIT;

                sync();
            }
        }

        if (world.isClient()) {
            if (this.rocket != null) {
                this.rocket.addParticles();
            }
        }
    }

    public void performAction(int action, int data) {
        if (world instanceof ServerWorld sWorld) {
            for (var player : sWorld.getPlayers()) {
                sWorld.sendToPlayerIfNearby(player,
                        true, this.getPos().getX(), this.getPos().getY(), this.getPos().getZ(),
                        PayloadPackets.pktSatelliteAction(this, action, data));
            }
        }

        switch (action) {
            case ACTION_LAUNCH -> {
                if (this.rocket == null) {
                    this.rocket = dormantRocket();
                }

                this.status = Status.LAUNCHING;
                this.rocket.inFlight = true;

                if (world instanceof ServerWorld) {
                    int channel = MathHelper.clamp(data, 0, RadioStorage.SATELLITE_CHANNEL_COUNT - 1);
                    this.setAndUpdateChannel(channel);
                }
            }
            case ACTION_CRASH -> {
                this.status = Status.NONE;

                spawnCrashingSatellite();
            }
        }

        if (!world.isClient()) {
            markDirty();
        }
    }

    public @Nullable Rocket getRocket() {
        return this.rocket;
    }

    public Status getStatus() {
        return this.status;
    }

    public Vec3d launchpadPos() {
        if (this.launchpadPos == null) {
            this.launchpadPos = Vec3d.ofBottomCenter(this.getPos())
                    .add(new Vec3d(-0.21875, 0.4375, -0.21875)
                            .rotateY((float) Math.toRadians(180 - this.getRotation().asRotation())));
        }

        return this.launchpadPos;
    }

    public void onDestroyed() {
        if (world instanceof ServerWorld sWorld) {
            RadarPoints.get(sWorld).remove(RadioStorage.toSatelliteBand(this.getChannel()), this.pos);
            if (status == Status.IN_ORBIT) {
                spawnCrashingSatellite();
            } else if (this.rocket != null) {
                var launchpad = this.launchpadPos();

                double rX = launchpad.x + rocket.x;
                double rY = launchpad.y + rocket.y;
                double rZ = launchpad.z + rocket.z;

                var item = new ItemEntity(world, rX, rY, rZ, PhonosItems.SATELLITE.getDefaultStack());
                world.spawnEntity(item);
            }
        }
    }

    public void spawnCrashingSatellite() {
        var pos = this.somewhereInTheSky().add(this.getPos().toCenterPos()).subtract(0, 100, 0);

        // TODO: Special crashing entity
        var item = new ItemEntity(world, pos.x, pos.y, pos.z, PhonosItems.SATELLITE.getDefaultStack());
        world.spawnEntity(item);
    }

    @Override
    public void readNbt(NbtCompound nbt) {
        super.readNbt(nbt);

        this.status = Status.values()[nbt.getInt("status")];

        if (nbt.contains("Rocket")) {
            this.rocket = rocketFromNbt(nbt.getCompound("Rocket"));
        } else {
            this.rocket = null;
        }

        for (int i = 0; i < inputs.length; i++) {
            inputs[i] = nbt.getBoolean("Input" + i);
        }

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

        nbt.putInt("status", this.status.ordinal());

        if (this.rocket != null) {
            var rocketNbt = new NbtCompound();
            this.rocket.writeNbt(rocketNbt);
            nbt.put("Rocket", rocketNbt);
        }

        for (int i = 0; i < inputs.length; i++) {
            nbt.putBoolean("Input" + i, inputs[i]);
        }

        nbt.putInt("channel", this.getChannel());
    }

    @Override
    public NbtCompound toInitialChunkDataNbt() {
        NbtCompound nbt = new NbtCompound();
        this.writeNbt(nbt);
        return nbt;
    }

    @Nullable
    @Override
    public Packet<ClientPlayPacketListener> toUpdatePacket() {
        return this.getPacket();
    }

    @Override
    public long emitterId() {
        return emitterId;
    }

    @Override
    public void forEachSource(Consumer<SoundSource> action) {}

    @Override
    public void forEachChild(LongConsumer action) {
        if (getStatus() == Status.IN_ORBIT)
            action.accept(UniqueId.ofSatelliteChannel(getChannel()));
    }

    @Override
    public boolean forwards() {
        return true;
    }

    public int getChannel() {
        return channel;
    }

    public void setAndUpdateChannel(int channel) {
        channel = Math.floorMod(channel, RadioStorage.SATELLITE_CHANNEL_COUNT);

        if (this.world instanceof ServerWorld sWorld) for (boolean in : this.inputs) if (in) {
            RadarPoints.get(sWorld).remove(RadioStorage.toSatelliteBand(this.channel), this.pos);
            RadarPoints.get(sWorld).add(RadioStorage.toSatelliteBand(channel), this.pos);
            break;
        }

        this.channel = channel;

        sync();
    }

    public boolean canLaunch(ServerPlayerEntity player) {
        return player.getPos().squaredDistanceTo(this.getPos().toCenterPos()) < 96 && player.canModifyBlocks() && getStatus().canLaunch();
    }

    public boolean canCrash(ServerPlayerEntity player) {
        return player.getPos().squaredDistanceTo(this.getPos().toCenterPos()) < 96 && player.canModifyBlocks() && getStatus().canCrash();
    }

    public Direction getRotation() {
        return this.getCachedState().get(Properties.HORIZONTAL_FACING);
    }

    public void tryOpenScreen(ServerPlayerEntity player) {
        if (this.status == Status.LAUNCHING) {
            player.sendMessageToClient(Text.translatable("message.phonos.satellite_launching").formatted(Formatting.GOLD), true);
        } else if (this.status == Status.IN_ORBIT) {
            PayloadPackets.sendOpenSatelliteStationScreen(player, pos, SCREEN_CRASH);
        } else if (this.rocket == null) {
            player.sendMessageToClient(Text.translatable("message.phonos.no_satellite").formatted(Formatting.RED), true);
        } else {
            PayloadPackets.sendOpenSatelliteStationScreen(player, pos, SCREEN_LAUNCH);
        }
    }

    public enum Status {
        NONE, IN_ORBIT, LAUNCHING;

        public boolean canLaunch() {
            return this == NONE;
        }

        public boolean canCrash() {
            return this == IN_ORBIT;
        }
    }

    public Vec3d somewhereInTheSky() {
        return new Vec3d(world.random.nextBetween(-5, 5), 200, world.random.nextBetween(-5, 5));
    }

    public Rocket dormantRocket() {
        return new Rocket(this.world.random.nextFloat() * 0.2, somewhereInTheSky());
    }

    public Rocket rocketFromNbt(NbtCompound nbt) {
        var rocket = new Rocket(nbt.getDouble("drift"), new Vec3d(
                nbt.getDouble("targX"), nbt.getDouble("targY"), nbt.getDouble("targZ")
        ));

        rocket.prevX = nbt.getDouble("prevX");
        rocket.prevY = nbt.getDouble("prevY");
        rocket.prevZ = nbt.getDouble("prevZ");
        rocket.x = nbt.getDouble("x");
        rocket.y = nbt.getDouble("y");
        rocket.z = nbt.getDouble("z");
        rocket.vel = nbt.getDouble("vel");
        rocket.inFlight = nbt.getBoolean("flying");

        return rocket;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " {(%s) %s satellite channel#%s}".formatted(getPos().toShortString(), UniqueId.debugNameOf(emitterId()), getChannel());
    }

    // All coordinates are relative to the block's "launchpad", not world coords
    public class Rocket {
        private final Vector3d calc = new Vector3d();

        public double prevX, prevY, prevZ;
        public double x, y, z;
        public double vel;
        public boolean inFlight;
        public boolean removed;

        public final double drift;
        private final Vector3d target;

        public Rocket(double drift, Vec3d target) {
            this.drift = drift;
            this.target = new Vector3d(target.x, target.y, target.z);
        }

        public void writeNbt(NbtCompound nbt) {
            nbt.putDouble("prevX", prevX);
            nbt.putDouble("prevY", prevY);
            nbt.putDouble("prevZ", prevZ);
            nbt.putDouble("x", x);
            nbt.putDouble("y", y);
            nbt.putDouble("z", z);
            nbt.putDouble("vel", vel);
            nbt.putBoolean("flying", inFlight);
            nbt.putDouble("drift", drift);
            nbt.putDouble("targX", target.x);
            nbt.putDouble("targY", target.y);
            nbt.putDouble("targZ", target.z);
        }

        public float getX(float delta) {
            return (float) MathHelper.lerp(delta, prevX, x);
        }

        public float getY(float delta) {
            return (float) MathHelper.lerp(delta, prevY, y);
        }

        public float getZ(float delta) {
            return (float) MathHelper.lerp(delta, prevZ, z);
        }

        public void tick() {
            if (inFlight) {
                this.prevX = x;
                this.prevY = y;
                this.prevZ = z;

                this.vel = Math.min(this.vel + 0.25, 3.25);

                calc.set(target.x - x, target.y - y, target.z - z).normalize(vel);

                this.x += calc.x;
                this.y += calc.y;
                this.z += calc.z;

                this.target.add(1 * drift, 0, 1 * drift);

                if (this.y > target.y) {
                    this.removed = true;
                }
            }
        }

        public void addParticles() {
            if (inFlight) {
                var be = SatelliteStationBlockEntity.this;

                if (be.world.isClient()) {
                    double x = be.launchpadPos().x + this.x;
                    double y = be.launchpadPos().y + this.y;
                    double z = be.launchpadPos().z + this.z;

                    MinecraftClient.getInstance().particleManager.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x, y, z, 0, -0.03 ,0);
                }
            }
        }
    }
}
