package io.github.foundationgames.phonos.item;

import io.github.foundationgames.phonos.network.ClientPayloadPackets;
import io.github.foundationgames.phonos.sound.SoundStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitter;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterStorage;
import io.github.foundationgames.phonos.sound.emitter.SoundEmitterTree;
import io.github.foundationgames.phonos.util.UniqueId;
import io.github.foundationgames.phonos.world.sound.data.SoundEventSoundData;
import net.minecraft.block.jukebox.JukeboxSong;
import net.minecraft.client.gui.screen.ingame.CreativeInventoryScreen;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.inventory.StackReference;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.screen.slot.Slot;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ClickType;
import net.minecraft.util.Formatting;
import net.minecraft.world.World;

import java.util.List;
import java.util.Optional;

public class PortableRecordPlayerItem extends Item implements SoundEmitterItem {
    public static final Text NO_DISC = Text.translatable("tooltip.phonos.item.no_disc").formatted(Formatting.RED);
    public static final Text HOW_TO_PLAY = Text.translatable("tooltip.phonos.item.record_player_hint").formatted(Formatting.GRAY, Formatting.ITALIC);
    public static final Text PLAYING = Text.translatable("tooltip.phonos.item.playing").formatted(Formatting.GOLD);

    public PortableRecordPlayerItem(Settings settings) {
        super(settings);
    }

    @Override
    public boolean onClicked(ItemStack stack, ItemStack otherStack, Slot slot, ClickType clickType, PlayerEntity player, StackReference cursorStackReference) {
        if (clickType == ClickType.RIGHT) {
            boolean result = false;
            boolean needsSync = player.isCreative() && player.getWorld().isClient();
            ItemStack sync = null;

            if (hasRecord(stack)) {
                if (otherStack.isEmpty()) {
                    if (needsSync) {
                        sync = stack.copy();
                    }

                    cursorStackReference.set(this.removeRecordAndStop(stack, player.getWorld()));
                    result = true;
                }
            } else {
                Optional<RegistryEntry<JukeboxSong>> optionalSong = JukeboxSong.getSongEntryFromStack(player.getRegistryManager(), otherStack);
                if (optionalSong.isPresent()) {
                    if (!hasEmitterId(stack)) {
                        refreshEmitterId(stack);
                    }

                    if (needsSync) {
                        sync = stack.copy();
                    }

                    this.putRecordAndPlay(stack, optionalSong.get().value(), otherStack, player.getWorld());
                    cursorStackReference.set(ItemStack.EMPTY);
                    result = true;
                }
            }

            if (sync != null) {
                if (player.currentScreenHandler instanceof CreativeInventoryScreen.CreativeScreenHandler) {
                    ClientPayloadPackets.sendFakeCreativeSlotClick(sync, otherStack, clickType);
                }
            }

            return result;
        }

        return super.onClicked(stack, otherStack, slot, clickType, player, cursorStackReference);
    }

    public boolean hasEmitterId(ItemStack stack) {
        return stack.contains(PhonosDataComponents.EMITTER_ID);
    }

    public long getEmitterId(ItemStack stack) {
        return stack.getOrDefault(PhonosDataComponents.EMITTER_ID, 0L);
    }

    public long refreshEmitterId(ItemStack stack) {
        long id = UniqueId.random();
        stack.set(PhonosDataComponents.EMITTER_ID, id);

        return id;
    }

    public void removeEmitterId(ItemStack stack) {
        stack.remove(PhonosDataComponents.EMITTER_ID);
    }

    public boolean hasRecord(ItemStack stack) {
        return !stack.getOrDefault(PhonosDataComponents.PORTABLE_RECORD_CONTENTS, ItemStack.EMPTY).isEmpty();
    }

    private void putRecordAndPlay(ItemStack stack, JukeboxSong song, ItemStack record, World world) {
        stack.set(PhonosDataComponents.PORTABLE_RECORD_CONTENTS, record);
        long emitterId = getEmitterId(stack);

        if (!world.isClient()) {
            SoundEmitterStorage.getInstance(world).addEmitter(SoundEmitter.noOp(emitterId));
            SoundStorage.getInstance(world).play(world, SoundEventSoundData.create(
                            emitterId, song.soundEvent(), SoundCategory.RECORDS, 2, 1),
                    new SoundEmitterTree(emitterId));
        }
    }

    public ItemStack getRecord(ItemStack stack) {
        return stack.getOrDefault(PhonosDataComponents.PORTABLE_RECORD_CONTENTS, ItemStack.EMPTY);
    }

    public ItemStack removeRecordAndStop(ItemStack stack, World world) {
        var record = stack.remove(PhonosDataComponents.PORTABLE_RECORD_CONTENTS);
        if (record != null && !record.isEmpty()) {
            if (!world.isClient() && hasEmitterId(stack)) {
                long emitterId = getEmitterId(stack);
                SoundEmitterStorage.getInstance(world).removeEmitter(emitterId);
                SoundStorage.getInstance(world).stop(world, emitterId);
            }

            removeEmitterId(stack);

            return record;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public void onItemEntityDestroyed(ItemEntity entity) {
        super.onItemEntityDestroyed(entity);

        var world = entity.getWorld();

        if (!world.isClient()) {
            var stack = entity.getStack();
            var disc = removeRecordAndStop(stack, world);

            if (!disc.isEmpty()) {
                entity.getWorld().spawnEntity(new ItemEntity(world, entity.getX(), entity.getY(), entity.getZ(), disc));
            }
        }
    }

    @Override
    public boolean hasParentEmitter(ItemStack stack) {
        return hasEmitterId(stack);
    }

    @Override
    public boolean createsEmitter(ItemStack stack) {
        return true;
    }

    @Override
    public long getParentEmitter(ItemStack stack) {
        return getEmitterId(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        super.appendTooltip(stack, context, tooltip, type);

        var record = this.getRecord(stack);
        var optionalSong = JukeboxSong.getSongEntryFromStack(context.getRegistryLookup(), record);
        if (optionalSong.isPresent()) {
            tooltip.add(PLAYING);
            tooltip.add(optionalSong.get().value().description().copy().formatted(Formatting.BLUE));
            tooltip.add(TOOLTIP_HINT);
        } else {
            tooltip.add(NO_DISC);
            tooltip.add(HOW_TO_PLAY);
        }
    }
}
