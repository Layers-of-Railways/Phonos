package io.github.foundationgames.phonos.world.command;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.AbstractOutputBlockEntity;
import io.github.foundationgames.phonos.block.entity.EnderMusicBoxBlockEntity;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.world.RadarPoints;
import net.fabricmc.fabric.api.command.v2.ArgumentTypeRegistry;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.command.argument.BlockPosArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.command.argument.serialize.ConstantArgumentSerializer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.ClickEvent;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Text;
import net.minecraft.text.Texts;
import net.minecraft.util.Formatting;
import net.minecraft.util.math.BlockPos;

import static net.minecraft.server.command.CommandManager.argument;
import static net.minecraft.server.command.CommandManager.literal;

public class PhonosCommands {
    public static void init() {
        ArgumentTypeRegistry.registerArgumentType(Phonos.id("satellite"),
                SatelliteArgumentType.class, ConstantArgumentSerializer.of(SatelliteArgumentType::new));

        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
            LiteralArgumentBuilder<ServerCommandSource> phonos = literal("phonos");

            phonos
                .then($radar())
                .then($ender_music_box());

            if (Phonos.DEBUG) {
                phonos.then($debug());
            }

            dispatcher.register(phonos);
        });
    }

    private static LiteralArgumentBuilder<ServerCommandSource> $radar() {
        return literal("radar")
            .requires(src -> src.hasPermissionLevel(2))
            .then(argument(
                "channel",
                IntegerArgumentType.integer(0, RadioStorage.CHANNEL_COUNT - 1))
                .executes(ctx -> radar(
                    ctx.getSource(),
                    ctx.getArgument("channel", Integer.class)
                ))
            );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> $ender_music_box() {
        return literal("ender_music_box")
            .then(literal("inspect").then(argument("pos", BlockPosArgumentType.blockPos())
                    .executes(ctx -> enderMusicBoxInspect(
                        ctx.getSource(),
                        ctx.getArgument("pos", PosArgument.class).toAbsoluteBlockPos(ctx.getSource())
                    ))
                )
            )
            .then(literal("list")
                .requires(src -> src.hasPermissionLevel(2))
                .executes(ctx -> enderMusicBoxList(
                    ctx.getSource()
                ))
            );
    }

    private static LiteralArgumentBuilder<ServerCommandSource> $debug() {
        return literal("debug")
            .requires(src -> src.hasPermissionLevel(2))
            .then($debug$network());
    }

    private static LiteralArgumentBuilder<ServerCommandSource> $debug$network() {
        return literal("network")
            .then(argument("source", BlockPosArgumentType.blockPos())
                .executes(ctx -> debugNetwork(
                    ctx.getSource(),
                    ctx.getArgument("source", PosArgument.class).toAbsoluteBlockPos(ctx.getSource())
                ))
            );
    }

    private static int debugNetwork(ServerCommandSource source, BlockPos sourcePos) {
        var world = source.getWorld();

        if (!(world.getBlockEntity(sourcePos) instanceof AbstractOutputBlockEntity be)) {
            source.sendError(Text.literal("Block at position is not an AbstractOutputBlockEntity"));
            return 0;
        }

        be.debugNetwork(world, msg -> source.sendFeedback(() -> Text.of(msg), false));
        return 1;
    }

    public static int enderMusicBoxInspect(ServerCommandSource source, BlockPos pos) {
        var world = source.getWorld();

        if (!ServerCustomAudio.loaded()) {
            source.sendError(Text.translatable("command.phonos.ender_music_box.not_loaded"));

            return 1;
        }

        if (world.getBlockEntity(pos) instanceof EnderMusicBoxBlockEntity be) {
            boolean[] any = {false};

            be.forEachStream((id, name) -> {
                var aud = ServerCustomAudio.SAVED.get((long) id);
                if (aud == null)
                    return;

                double sizeKB = (double)(aud.originalSize / 100) / 10D;
                int duration = (int) Math.ceil((double) aud.originalSize / aud.sampleRate);
                source.sendMessage(Text.translatable("command.phonos.ender_music_box.entry.named",
                    name,
                    Long.toHexString(id),
                    PhonosUtil.duration(duration),
                    sizeKB));
                any[0] = true;
            });

            if (!any[0]) {
                source.sendError(Text.translatable("command.phonos.ender_music_box.inspect.no_upload"));
            }

            return 1;
        }

        source.sendError(Text.translatable("command.phonos.ender_music_box.inspect.invalid"));
        return 1;
    }

    public static int enderMusicBoxList(ServerCommandSource source) {
        if (!ServerCustomAudio.loaded()) {
            source.sendError(Text.translatable("command.phonos.ender_music_box.not_loaded"));

            return 1;
        }

        var set = ServerCustomAudio.SAVED.long2ObjectEntrySet();

        if (set.isEmpty()) {
            source.sendError(Text.translatable("command.phonos.ender_music_box.list.none"));

            return 1;
        }

        double totalSizeKB = 0;

        for (var entry : set) {
            double sizeKB = (double)(entry.getValue().originalSize / 100) / 10D;
            int duration = (int) Math.ceil((double) entry.getValue().originalSize / entry.getValue().sampleRate);
            source.sendMessage(Text.translatable("command.phonos.ender_music_box.entry",
                    Long.toHexString(entry.getLongKey()),
                    PhonosUtil.duration(duration),
                    sizeKB));
            totalSizeKB += entry.getValue().originalSize;
        }

        totalSizeKB = (double)((int)totalSizeKB / 100) / 10D;
        source.sendMessage(Text.translatable("command.phonos.ender_music_box.list.info", set.size(), totalSizeKB));

        return 1;
    }

    public static int radar(ServerCommandSource source, int channel) {
        var world = source.getWorld();
        var origin = source.getPosition();

        var radar = RadarPoints.get(world);
        var pos = new BlockPos.Mutable();

        var result = new BlockPos.Mutable();
        double minSqDist = Double.POSITIVE_INFINITY;

        var points = radar.getPoints(channel);
        if (points == null || points.size() == 0) {
            source.sendError(Text.translatable("command.phonos.radar.none_found", channel));

            return 1;
        }

        for (long l : radar.getPoints(channel)) {
            pos.set(l);

            double sqDist = origin.squaredDistanceTo(pos.getX(), pos.getY(), pos.getZ());
            if (sqDist < minSqDist) {
                result.set(pos);
                minSqDist = sqDist;
            }
        }

        sendCoordinates(source, "command.phonos.radar.success", result.up());

        return 1;
    }

    private static void sendCoordinates(ServerCommandSource source, String key, BlockPos pos) {
        Text coords = Texts.bracketed(
                Text.translatable("chat.coordinates", pos.getX(), pos.getY(), pos.getZ())
        ).styled(style ->
                style.withColor(Formatting.GREEN)
                        .withClickEvent(
                                new ClickEvent(ClickEvent.Action.SUGGEST_COMMAND, "/tp @s " + pos.getX() + " " + pos.getY() + " " + pos.getZ()))
                        .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.translatable("chat.coordinates.tooltip"))));

        source.sendFeedback(() -> Text.translatable(key, coords), false);
    }
}
