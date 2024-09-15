package io.github.foundationgames.phonos.world.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.AbstractOutputBlockEntity;
import io.github.foundationgames.phonos.client.render.RadioDebugRenderer;
import io.github.foundationgames.phonos.util.SoundTimer;
import io.github.foundationgames.phonos.world.command.ClientBlockPosArgumentType.ClientPosArgument;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.CommandSource;
import net.minecraft.command.argument.IdentifierArgumentType;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.item.Item;
import net.minecraft.item.MusicDiscItem;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import net.minecraft.util.math.BlockPos;

import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class PhonosClientCommands {
    public static void initClient() {
        Phonos.LOG.info("Registering Phonos client commands");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> phonosClient = literal("phonos_client");

            phonosClient
                .then($radio_dbg())
                .then($dump_sound_lengths());

            if (Phonos.DEBUG) {
                phonosClient.then($debug());
            }

            dispatcher.register(phonosClient);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $radio_dbg() {
        return literal("radio_dbg")
            .then($radio_dbg$add())
            .then($radio_dbg$remove())
            .then($radio_dbg$clear());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $radio_dbg$add() {
        return literal("add")
            .then(argument("pos", ClientBlockPosArgumentType.blockPosClient())
                .executes(ctx -> {
                    var pos = ((ClientPosArgument) ctx.getArgument("pos", PosArgument.class)).toAbsoluteBlockPos(ctx.getSource());
                    RadioDebugRenderer.addTarget(pos);
                    return 1;
                })
            );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $radio_dbg$remove() {
        return literal("remove")
            .then(argument("pos", ClientBlockPosArgumentType.blockPosClient())
                .executes(ctx -> {
                    var pos = ((ClientPosArgument) ctx.getArgument("pos", PosArgument.class)).toAbsoluteBlockPos(ctx.getSource());
                    RadioDebugRenderer.removeTarget(pos);
                    return 1;
                })
            );
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $radio_dbg$clear() {
        return literal("clear")
            .executes(ctx -> {
                RadioDebugRenderer.clearTargets();
                return 1;
            });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $debug() {
        return literal("debug")
            .then($debug$network())
            .then($debug$sound_length());
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $debug$network() {
        return literal("network")
            .then(argument("source", ClientBlockPosArgumentType.blockPosClient())
                .executes(ctx -> debugNetworkClient(
                    ctx.getSource(),
                    ((ClientPosArgument) ctx.getArgument("source", PosArgument.class)).toAbsoluteBlockPos(ctx.getSource())
                ))
            );
    }

    private static int debugNetworkClient(FabricClientCommandSource source, BlockPos sourcePos) {
        var world = MinecraftClient.getInstance().world;

        if (world == null || !(world.getBlockEntity(sourcePos) instanceof AbstractOutputBlockEntity be)) {
            source.sendError(Text.literal("Block at position is not an AbstractOutputBlockEntity"));
            return 0;
        }

        be.debugNetwork(world, msg -> source.sendFeedback(Text.of(msg)));
        return 1;
    }

    private static final SuggestionProvider<FabricClientCommandSource> AVAILABLE_SOUNDS = (context, builder) -> CommandSource.suggestIdentifiers(context.getSource().getSoundIds(), builder);

    private static LiteralArgumentBuilder<FabricClientCommandSource> $debug$sound_length() {
        return literal("sound_length")
            .then(argument("sound", IdentifierArgumentType.identifier()).suggests(AVAILABLE_SOUNDS)
                .executes(ctx -> debugSoundLengthClient(
                    ctx.getSource(),
                    ctx.getArgument("sound", Identifier.class)
                ))
            );
    }

    private static int debugSoundLengthClient(FabricClientCommandSource source, Identifier sound) {
        CompletableFuture.runAsync(() -> {
            float length;
            try {
                length = SoundTimer.getSoundLength(sound);
            } catch (IOException e) {
                source.sendError(Text.of("Error loading sound: %s".formatted(e.getMessage())));
                return;
            }
            source.sendFeedback(Text.of("Sound length of %s: %s".formatted(sound, Math.ceil(length) + "s")));
        }, Util.getMainWorkerExecutor());

        return 1;
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $dump_sound_lengths() {
        return literal("dump_sound_lengths")
            .executes(ctx -> dumpSoundLengthsClient(ctx.getSource()));
    }

    private static int dumpSoundLengthsClient(FabricClientCommandSource source) {
        int found = 0;

        Path basePath = FabricLoader.getInstance().getGameDir().resolve("phonos_generated/sound_lengths");
        if (!basePath.toFile().exists() && !basePath.toFile().mkdirs()) {
            source.sendError(Text.of("Error creating directory: %s".formatted(basePath)));
            return 0;
        }

        Path mcmetaPath = basePath.resolve("pack.mcmeta");
        if (!write(mcmetaPath, "{\n  \"pack\": {\n    \"pack_format\": 15,\n    \"description\": \"Phonos generated sound lengths\"\n  }\n}")) {
            source.sendError(Text.of("Error writing pack.mcmeta"));
            return 0;
        }

        Path dataPath = basePath.resolve("data/phonos/sound_length_overrides");
        if (!dataPath.toFile().exists() && !dataPath.toFile().mkdirs()) {
            source.sendError(Text.of("Error creating directory: %s".formatted(dataPath)));
            return 0;
        }

        for (var entry : Registries.ITEM.getIndexedEntries()) {
            Item item = entry.value();
            if (!(item instanceof MusicDiscItem musicDisc)) continue;

            Identifier itemID = entry.getKey().get().getValue();

            Identifier sound = musicDisc.getSound().getId();
            CompletableFuture.runAsync(() -> {
                float length;
                try {
                    length = SoundTimer.getSoundLength(sound);
                } catch (IOException e) {
                    source.sendError(Text.of("Error loading sound: %s".formatted(e.getMessage())));
                    return;
                }

                int actualTickLength = Math.round(length) * 20;
                int reportedLength = musicDisc.getSongLengthInTicks();

                if (actualTickLength == reportedLength) {
                    return;
                }
                source.sendFeedback(Text.of("Sound length of %s: (reported: %s ticks) (actual: %s ticks)".formatted(sound, reportedLength, actualTickLength)));

                Path modPath = dataPath.resolve(itemID.getNamespace());
                if (!modPath.toFile().exists() && !modPath.toFile().mkdirs()) {
                    source.sendError(Text.of("Error creating directory: %s".formatted(modPath)));
                    return;
                }

                Path soundPath = modPath.resolve(itemID.getPath() + ".json");
                if (!write(soundPath, "{\n  \"length\": %s\n}".formatted(actualTickLength))) {
                    source.sendError(Text.of("Error writing file: %s".formatted(soundPath)));
                    return;
                }
            }, Util.getMainWorkerExecutor());
        }

        return found;
    }

    private static boolean write(Path path, String contents) {
        try(FileWriter writer = new FileWriter(path.toFile());) {
            writer.write(contents);
            return true;
        } catch (IOException e) {
            Phonos.LOG.error("Error writing file", e);
            return false;
        }
    }
}
