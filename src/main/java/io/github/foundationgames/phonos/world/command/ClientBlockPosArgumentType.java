package io.github.foundationgames.phonos.world.command;

import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.minecraft.command.argument.*;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;

@Environment(EnvType.CLIENT)
public class ClientBlockPosArgumentType extends BlockPosArgumentType {
    public static ClientBlockPosArgumentType blockPosClient() {
        return new ClientBlockPosArgumentType();
    }

    @Override
    public PosArgument parse(StringReader stringReader) throws CommandSyntaxException {
        return ClientPosArgument.parse(stringReader);
    }

    @Environment(EnvType.CLIENT)
    public static class ClientPosArgument extends DefaultPosArgument {

        private final CoordinateArgument x;
        private final CoordinateArgument y;
        private final CoordinateArgument z;

        public ClientPosArgument(CoordinateArgument x, CoordinateArgument y, CoordinateArgument z) {
            super(x, y, z);
            this.x = x;
            this.y = y;
            this.z = z;
        }

        public Vec3d toAbsolutePos(FabricClientCommandSource source) {
            Vec3d vec3d = source.getPosition();
            return new Vec3d(this.x.toAbsoluteCoordinate(vec3d.x), this.y.toAbsoluteCoordinate(vec3d.y), this.z.toAbsoluteCoordinate(vec3d.z));
        }

        public BlockPos toAbsoluteBlockPos(FabricClientCommandSource source) {
            return BlockPos.ofFloored(this.toAbsolutePos(source));
        }

        public static ClientPosArgument parse(StringReader reader) throws CommandSyntaxException {
            int i = reader.getCursor();
            CoordinateArgument coordinateArgument = CoordinateArgument.parse(reader);
            if (!reader.canRead() || reader.peek() != ' ') {
                reader.setCursor(i);
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
            reader.skip();
            CoordinateArgument coordinateArgument2 = CoordinateArgument.parse(reader);
            if (!reader.canRead() || reader.peek() != ' ') {
                reader.setCursor(i);
                throw Vec3ArgumentType.INCOMPLETE_EXCEPTION.createWithContext(reader);
            }
            reader.skip();
            CoordinateArgument coordinateArgument3 = CoordinateArgument.parse(reader);
            return new ClientPosArgument(coordinateArgument, coordinateArgument2, coordinateArgument3);
        }
    }
}
