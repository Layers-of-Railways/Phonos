package io.github.foundationgames.phonos.item;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.github.foundationgames.phonos.world.sound.InputPlugPoint;
import io.github.foundationgames.phonos.world.sound.block.InputBlock;
import io.github.foundationgames.phonos.world.sound.block.OutputBlockEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.util.ActionResult;
import net.minecraft.util.DyeColor;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.ChunkSectionPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AudioCableItem extends Item {
    public final @Nullable DyeColor color;

    public AudioCableItem(@Nullable DyeColor color, Settings settings) {
        super(settings);
        this.color = color;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        var world = context.getWorld();

        if (world.isClient() || (context.getPlayer() != null && !context.getPlayer().canModifyBlocks())) {
            return ActionResult.PASS;
        }

        var pos = context.getBlockPos();
        var hitPos = context.getHitPos();
        var stack = context.getStack();

        boolean hasInput = stack.contains(PhonosDataComponents.AUDIO_CABLE_INPUT);
        boolean hasOutput = stack.contains(PhonosDataComponents.AUDIO_CABLE_OUTPUT);
        boolean deplete = context.getPlayer() == null || !context.getPlayer().isCreative();

        if (world.getBlockEntity(pos) instanceof OutputBlockEntity outputs && outputs.canConnect(context)) {
            stack.set(PhonosDataComponents.AUDIO_CABLE_OUTPUT, new CableConnectionPoint(pos, hitPos));

            return this.tryCreateConnection(stack, world, deplete);
        } else if (world.getBlockState(pos).getBlock() instanceof InputBlock inputs && inputs.canInputConnect(context)) {
            stack.set(PhonosDataComponents.AUDIO_CABLE_INPUT, new CableConnectionPoint(pos, hitPos));

            return this.tryCreateConnection(stack, world, deplete);
        } else if (hasInput || hasOutput) {
            stack.remove(PhonosDataComponents.AUDIO_CABLE_INPUT);
            stack.remove(PhonosDataComponents.AUDIO_CABLE_OUTPUT);
        }

        return super.useOnBlock(context);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        // TODO sometime
        return super.useOnEntity(stack, user, entity, hand);
    }

    public ActionResult tryCreateConnection(ItemStack stack, World world, boolean deplete) {
        var inputPoint = stack.get(PhonosDataComponents.AUDIO_CABLE_INPUT);
        var outputPoint = stack.get(PhonosDataComponents.AUDIO_CABLE_OUTPUT);
        if (inputPoint != null && outputPoint != null) {
            InputPlugPoint inputPlug = null;

            {
                int x = inputPoint.pos().getX();
                int z = inputPoint.pos().getZ();
                if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
                    var state = world.getBlockState(inputPoint.pos());

                    if (state.getBlock() instanceof InputBlock block) {
                        int index = block.getInputLayout().getClosestIndexClicked(inputPoint.hitPos(), inputPoint.pos(), block.getRotation(state));

                        if (index >= 0) {
                            inputPlug = block.getInputLayout().inputOfConnection(
                                    InputPlugPoint.BLOCK_TYPE, inputPoint.pos(), index);
                        }
                    }
                }
            }

            if (inputPlug != null) {
                int x = outputPoint.pos().getX();
                int z = outputPoint.pos().getZ();
                if (world.isChunkLoaded(ChunkSectionPos.getSectionCoord(x), ChunkSectionPos.getSectionCoord(z))) {
                    var cableStack = deplete ? stack.split(1) : stack.copyWithCount(1);

                    cableStack.remove(PhonosDataComponents.AUDIO_CABLE_INPUT);
                    cableStack.remove(PhonosDataComponents.AUDIO_CABLE_OUTPUT);

                    if (world.getBlockEntity(outputPoint.pos()) instanceof OutputBlockEntity entity &&
                            entity.addConnection(outputPoint.hitPos(), this.color, inputPlug, cableStack)) {
                        inputPlug.setConnected(world, true);
                        stack.remove(PhonosDataComponents.AUDIO_CABLE_INPUT);
                        stack.remove(PhonosDataComponents.AUDIO_CABLE_OUTPUT);

                        return ActionResult.SUCCESS;
                    } else {
                        stack.remove(PhonosDataComponents.AUDIO_CABLE_INPUT);
                        stack.remove(PhonosDataComponents.AUDIO_CABLE_OUTPUT);
                    }
                }
            }
        }

        return ActionResult.FAIL;
    }

    public record CableConnectionPoint(BlockPos pos, Vec3d hitPos) {
        public static final Codec<CableConnectionPoint> CODEC = RecordCodecBuilder.create(i -> i.group(
                BlockPos.CODEC.fieldOf("pos").forGetter(CableConnectionPoint::pos),
                Vec3d.CODEC.fieldOf("hitPos").forGetter(CableConnectionPoint::hitPos)
        ).apply(i, CableConnectionPoint::new));

        public static final PacketCodec<PacketByteBuf, CableConnectionPoint> PACKET_CODEC = PacketCodec.tuple(
            BlockPos.PACKET_CODEC,
            CableConnectionPoint::pos,
            PacketCodec.tuple(
                PacketCodecs.DOUBLE,
                Vec3d::getX,
                PacketCodecs.DOUBLE,
                Vec3d::getY,
                PacketCodecs.DOUBLE,
                Vec3d::getZ,
                Vec3d::new
            ),
            CableConnectionPoint::hitPos,
            CableConnectionPoint::new
        );
    }
}
