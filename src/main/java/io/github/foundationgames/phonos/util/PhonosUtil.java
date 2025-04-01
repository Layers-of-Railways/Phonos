package io.github.foundationgames.phonos.util;

import io.github.foundationgames.phonos.block.entity.Ticking;
import io.github.foundationgames.phonos.item.AudioCableItem;
import it.unimi.dsi.fastutil.objects.Object2IntArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.BlockEntityTicker;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.LightmapTextureManager;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.Inventory;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.WorldSavePath;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.RotationAxis;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.file.Path;
import java.text.NumberFormat;
import java.util.Locale;
import java.util.function.IntFunction;
import java.util.function.Supplier;

public enum PhonosUtil {;
    public static final float SQRT2DIV2 = (float) (Math.sqrt(2) / 2);

    public static final Object2IntMap<DyeColor> DYE_COLORS = new Object2IntArrayMap<>();
    public static Quaternionf rotationTo(Direction direction) {
        return switch (direction) {
            case NORTH -> RotationAxis.POSITIVE_Y.rotationDegrees(0);
            case EAST -> RotationAxis.POSITIVE_Y.rotationDegrees(270);
            case SOUTH -> RotationAxis.POSITIVE_Y.rotationDegrees(180);
            case WEST -> RotationAxis.POSITIVE_Y.rotationDegrees(90);
            case UP -> RotationAxis.POSITIVE_X.rotationDegrees(90);
            case DOWN -> RotationAxis.POSITIVE_X.rotationDegrees(270);
        };
    }

    public static Vec3d rotateTo(Vec3d vec, Direction dir) {
        return switch (dir) {
            case SOUTH -> vec.rotateY((float) Math.PI);
            case EAST -> vec.rotateY((float) Math.PI * 0.5f);
            case WEST -> vec.rotateY((float) Math.PI * -0.5f);
            case UP -> vec.rotateX((float) Math.PI * 0.5f);
            case DOWN -> vec.rotateX((float) Math.PI * -0.5f);
            default -> vec;
        };
    }

    public static int lerpLight(float delta, int packedA, int packedB) {
        return LightmapTextureManager.pack(
                MathHelper.lerp(delta,
                        LightmapTextureManager.getBlockLightCoordinates(packedA),
                        LightmapTextureManager.getBlockLightCoordinates(packedB)
                ),
                MathHelper.lerp(delta,
                        LightmapTextureManager.getSkyLightCoordinates(packedA),
                        LightmapTextureManager.getSkyLightCoordinates(packedB)
                )
        );
    }

    public static int slotOf(Inventory inv, ItemStack stack) {
        for (int i = 0; i < inv.size(); i++) {
            if(inv.getStack(i) == stack) return i;
        }
        return -1;
    }

    public static Vector4f vec3to4(Vector3f in, Vector4f out) {
        return out.set(in.x(), in.y(), in.z(), 1);
    }

    public static Vector3f vec4to3(Vector4f in, Vector3f out) {
        return out.set(in.x(), in.y(), in.z());
    }

    public static float pitchFromNote(int note) {
        return (float) Math.pow(2, (double)(note - 12) / 12);
    }
    public static int noteFromPitch(float pitch) {
        return (int) Math.round(17.3123404907 * Math.log(pitch) + 12);
    }

    public static int getColorFromNote(int note) {
        float d = (float)note/24;
        float r = Math.max(0.0F, MathHelper.sin((d + 0.0F) * 6.2831855F) * 0.65F + 0.35F);
        float g = Math.max(0.0F, MathHelper.sin((d + 0.33333334F) * 6.2831855F) * 0.65F + 0.35F);
        float b = Math.max(0.0F, MathHelper.sin((d + 0.6666667F) * 6.2831855F) * 0.65F + 0.35F);

        int ri = Math.round(r * 255f);
        int gi = Math.round(g * 255f);
        int bi = Math.round(b * 255f);
        return (ri & 255) << 16 | (gi & 255) << 8 | (bi & 255);
    }

    public static double maxSquaredConnectionDistance(World world) {
        return 200;
    }

    public static boolean noneNull(Object ... vals) {
        for (var val : vals) {
            if (val == null) return false;
        }
        return true;
    }

    public static boolean holdingAudioCable(PlayerEntity player) {
        return player.getStackInHand(Hand.MAIN_HAND).getItem() instanceof AudioCableItem ||
                player.getStackInHand(Hand.OFF_HAND).getItem() instanceof AudioCableItem;
    }

    @SuppressWarnings("unchecked")
    public static <E extends BlockEntity & Ticking, G extends BlockEntity> BlockEntityTicker<G> blockEntityTicker(BlockEntityType<G> givenType, BlockEntityType<E> expectedType) {
        return expectedType == givenType ? (BlockEntityTicker<G>) (BlockEntityTicker<E>) Ticking::ticker : null;
    }

    public static int brighten(int color, float factor) {
        int r = (color >> 16) & 0xFF;
        int g = (color >> 8) & 0xFF;
        int b = color & 0xFF;

        r += (255 - r) * factor;
        g += (255 - g) * factor;
        b += (255 - b) * factor;

        return b | (g << 8) | (r << 16);
    }

    public static void writeInt(OutputStream stream, int i) throws IOException {
        stream.write(i);
        stream.write(i >> 8);
        stream.write(i >> 16);
        stream.write(i >> 24);
    }

    public static int readInt(InputStream stream) throws IOException {
        int r = 0;
        r |= stream.read();
        r |= stream.read() << 8;
        r |= stream.read() << 16;
        r |= stream.read() << 24;

        return r;
    }

    public static String duration(int seconds) {
        var fmt = NumberFormat.getInstance(Locale.ROOT);
        fmt.setMinimumIntegerDigits(2);

        if (seconds < 60) {
            return "0:" + fmt.format(seconds);
        }

        int sec = seconds % 60;
        int min = (seconds / 60) % 60;

        if (seconds < 3600) {
            return min + ":" + fmt.format(sec);
        }

        int hr = seconds / 3600;

        return hr + ":" + fmt.format(min) + ":" + fmt.format(sec);
    }

    public static Path getCustomSoundFolder(MinecraftServer server) {
        return server.getSavePath(WorldSavePath.ROOT).resolve("phonos");
    }

    public static void writeBufferToPacket(PacketByteBuf packet, ByteBuffer buffer) {
        int bufferCur = buffer.position();
        int size = buffer.remaining();
        byte[] bytes = new byte[size];
        buffer.get(bytes);
        buffer.position(bufferCur);

        packet.writeByteArray(bytes);
    }

    public static ByteBuffer readBufferFromPacket(PacketByteBuf packet, IntFunction<ByteBuffer> create) {
        byte[] bytes = packet.readByteArray();
        var buffer = create.apply(bytes.length);
        buffer.put(bytes);
        buffer.flip();

        return buffer;
    }

    public static void runIfClient(Supplier<Runnable> runnable) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            runnable.get().run();
        }
    }

    public static <T> T getIfClient(Supplier<Supplier<T>> supplier) {
        return getIfClient(supplier, null);
    }

    public static <T> T getIfClient(Supplier<Supplier<T>> supplier, T defaultValue) {
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            return supplier.get().get();
        }
        return defaultValue;
    }

    @Environment(EnvType.CLIENT)
    private static World $getClientWorld() {
        return MinecraftClient.getInstance().world;
    }

    public static @Nullable World getClientWorld() {
        return getIfClient(() -> PhonosUtil::$getClientWorld, null);
    }

    static {
        for (var dye : DyeColor.values()) {
            int r = (int) (dye.getColorComponents()[0] * 0xFF);
            int g = (int) (dye.getColorComponents()[1] * 0xFF);
            int b = (int) (dye.getColorComponents()[2] * 0xFF);
            DYE_COLORS.put(dye, b | (g << 8) | (r << 16));
        }
    }
}
