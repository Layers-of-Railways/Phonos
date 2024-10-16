package io.github.foundationgames.phonos;

import io.github.foundationgames.phonos.block.PhonosBlocks;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.item.ItemGroupQueue;
import io.github.foundationgames.phonos.item.PhonosItems;
import io.github.foundationgames.phonos.mixin_interfaces.IMicrophoneHoldingServerPlayerEntity;
import io.github.foundationgames.phonos.network.PayloadPackets;
import io.github.foundationgames.phonos.radio.RadioDevice;
import io.github.foundationgames.phonos.radio.RadioStorage;
import io.github.foundationgames.phonos.recipe.ItemGlowRecipe;
import io.github.foundationgames.phonos.sound.MusicDiscOverrides;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.custom.ServerCustomAudio;
import io.github.foundationgames.phonos.sound.emitter.SecondaryEmitterHolder;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterStorage;
import io.github.foundationgames.phonos.sound.stream.ServerOutgoingStreamHandler;
import io.github.foundationgames.phonos.util.PhonosTags;
import io.github.foundationgames.phonos.util.PhonosUtil;
import io.github.foundationgames.phonos.util.ServerLifecycleHooks;
import io.github.foundationgames.phonos.world.command.PhonosCommands;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.data.SoundDataTypes;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerEntityWorldChangeEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerBlockEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseEntityCallback;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.block.DispenserBlock;
import net.minecraft.block.dispenser.FallibleItemDispenserBehavior;
import net.minecraft.item.ArmorItem;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.resource.ResourceType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPointer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.nio.file.Files;

public class Phonos implements ModInitializer {
    public static final Logger LOG = LogManager.getLogger("phonos");

    public static final ItemGroupQueue PHONOS_ITEMS = new ItemGroupQueue(id("phonos"));

    public static final Identifier STREAMED_SOUND = Phonos.id("streamed");

    public static final Identifier SVC_STREAMED_SOUND = Phonos.id("svc_streamed");

    public static final RecipeSerializer<ItemGlowRecipe> ITEM_GLOW_RECIPE_SERIALIZER = new SpecialRecipeSerializer<>(ItemGlowRecipe::new);

    public static final boolean DEBUG = false;

    @Override
    public void onInitialize() {
        Registry.register(Registries.ITEM_GROUP, PHONOS_ITEMS.id, FabricItemGroup.builder()
                .icon(PhonosBlocks.LOUDSPEAKER.asItem()::getDefaultStack)
                .displayName(PHONOS_ITEMS.displayName())
                .entries(PHONOS_ITEMS)
                .build());

        Registry.register(Registries.RECIPE_SERIALIZER, Phonos.id("crafting_special_itemglow"), ITEM_GLOW_RECIPE_SERIALIZER);

        PayloadPackets.initCommon();

        PhonosBlocks.init();
        PhonosItems.init();
        PhonosTags.init();

        SoundDataTypes.init();
        InputPlugPoint.init();

        ServerLifecycleEvents.SERVER_STARTING.register(e -> {
            RadioStorage.serverReset();
            SoundStorage.serverReset();
            SoundEmitterStorage.serverReset();
            ServerOutgoingStreamHandler.reset();
        });

        ServerLifecycleEvents.SERVER_STARTED.register(e -> {
            // Initialize config
            PhonosServerConfig.get(e.getOverworld());

            ServerCustomAudio.reset();
            try {
                var path = PhonosUtil.getCustomSoundFolder(e);
                if (!Files.exists(path)) Files.createDirectory(path);

                ServerCustomAudio.load(path);
            } catch (IOException ex) {
                Phonos.LOG.error("Error loading custom audio files", ex);
            }
        });
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) ->
                ServerCustomAudio.onPlayerDisconnect(handler.getPlayer()));

        ServerPlayConnectionEvents.JOIN.register(((handler, sender, server) -> {
            ServerPlayerEntity player = handler.getPlayer();
            SoundStorage.getInstance(player.getServerWorld())
                .registerPlayerWaitingForResume(player);
            PayloadPackets.sendConfig(player, PhonosServerConfig.getHandler(player.getServerWorld()));
        }));

        ServerEntityWorldChangeEvents.AFTER_PLAYER_CHANGE_WORLD.register(((player, origin, destination) -> {
            SoundStorage.getInstance(player.getServerWorld())
                .registerPlayerWaitingForResume(player);
        }));

        ServerTickEvents.END_WORLD_TICK.register(world -> SoundStorage.getInstance(world).tick(world));
        ServerTickEvents.START_SERVER_TICK.register(ServerOutgoingStreamHandler::tick);

        ServerBlockEntityEvents.BLOCK_ENTITY_LOAD.register((be, world) -> {
            if (be instanceof SoundEmitter p) {
                SoundEmitterStorage.getInstance(world).addEmitter(p);
            }
            if (be instanceof SecondaryEmitterHolder p) {
                SoundEmitterStorage.getInstance(world).addEmitter(p.getSecondaryEmitter());
            }
            if (be instanceof RadioDevice.Receiver rec) {
                rec.setAndUpdateChannel(rec.getChannel());
            }
        });
        ServerBlockEntityEvents.BLOCK_ENTITY_UNLOAD.register((be, world) -> {
            if (be instanceof SoundEmitter p) {
                SoundEmitterStorage.getInstance(world).removeEmitter(p);
            }
            if (be instanceof SecondaryEmitterHolder p) {
                SoundEmitterStorage.getInstance(world).removeEmitter(p.getSecondaryEmitter());
            }
            if (be instanceof RadioDevice.Receiver rec) {
                rec.removeReceiver();
            }
        });

        ResourceManagerHelper.get(ResourceType.SERVER_DATA).registerReloadListener(MusicDiscOverrides.ReloadListener.INSTANCE);

        DispenserBlock.registerBehavior(PhonosItems.HEADSET, new FallibleItemDispenserBehavior() {
            @Override
            protected ItemStack dispenseSilently(BlockPointer pointer, ItemStack stack) {
                this.setSuccess(ArmorItem.dispenseArmor(pointer, stack));
                return stack;
            }
        });

        UseEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (player instanceof IMicrophoneHoldingServerPlayerEntity sourceMHP && entity instanceof ServerPlayerEntity target && target instanceof IMicrophoneHoldingServerPlayerEntity targetMHP) {
                var sourceStation = sourceMHP.phonos$getBaseStation();
                if (sourceStation != null && targetMHP.phonos$getBaseStation() == null && sourceStation.canStart(target)) {
                    sourceStation.stop();
                    sourceStation.start(target);

                    player.sendMessage(Text.translatable("message.phonos.transferred_microphone_to", target.getDisplayName()), true);
                    target.sendMessage(Text.translatable("message.phonos.received_microphone_from", player.getDisplayName()), true);

                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });

        RadioStorage.init();
        PhonosCommands.init();
        ServerLifecycleHooks.init();
    }

    public static Identifier id(String path) {
        return new Identifier("phonos", path);
    }
}
