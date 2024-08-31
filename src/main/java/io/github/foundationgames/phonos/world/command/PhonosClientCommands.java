package io.github.foundationgames.phonos.world.command;

import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import io.github.foundationgames.phonos.Phonos;
import io.github.foundationgames.phonos.block.entity.AbstractOutputBlockEntity;
import io.github.foundationgames.phonos.block.entity.ElectronicNoteBlockEntity;
import io.github.foundationgames.phonos.world.command.ClientBlockPosArgumentType.ClientPosArgument;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.client.MinecraftClient;
import net.minecraft.command.argument.PosArgument;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.argument;
import static net.fabricmc.fabric.api.client.command.v2.ClientCommandManager.literal;

@Environment(EnvType.CLIENT)
public class PhonosClientCommands {
    public static void initClient() {
        Phonos.LOG.info("Registering Phonos client commands");
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, dedicated) -> {
            LiteralArgumentBuilder<FabricClientCommandSource> phonosClient = literal("phonos_client");

            phonosClient
                .then($debug());

            dispatcher.register(phonosClient);
        });
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> $debug() {
        return literal("debug")
            .then($debug$network());
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
}
