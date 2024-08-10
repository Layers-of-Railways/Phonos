package io.github.foundationgames.phonos.util;

import net.minecraft.util.function.BooleanBiFunction;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.Direction.AxisDirection;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;

import static net.minecraft.block.Block.createCuboidShape;
import static net.minecraft.util.math.Direction.UP;

// Copied from the Create mod, under the MIT license
public class VoxelShaper {

    private Map<Direction, VoxelShape> shapes = new HashMap<>();

    public VoxelShape get(Direction direction) {
        return shapes.get(direction);
    }

    public VoxelShape get(Axis axis) {
        return shapes.get(axisAsFace(axis));
    }

    public static VoxelShaper forHorizontal(VoxelShape shape, Direction facing) {
        return forDirectionsWithRotation(shape, facing, Direction.Type.HORIZONTAL, new HorizontalRotationValues());
    }

    public static VoxelShaper forHorizontalAxis(VoxelShape shape, Axis along) {
        return forDirectionsWithRotation(shape, axisAsFace(along), Arrays.asList(Direction.SOUTH, Direction.EAST),
            new HorizontalRotationValues());
    }

    public static VoxelShaper forDirectional(VoxelShape shape, Direction facing) {
        return forDirectionsWithRotation(shape, facing, Arrays.asList(Direction.values()), new DefaultRotationValues());
    }

    public static VoxelShaper forAxis(VoxelShape shape, Axis along) {
        return forDirectionsWithRotation(shape, axisAsFace(along),
            Arrays.asList(Direction.SOUTH, Direction.EAST, UP), new DefaultRotationValues());
    }

    public VoxelShaper withVerticalShapes(VoxelShape upShape) {
        shapes.put(UP, upShape);
        shapes.put(Direction.DOWN, rotatedCopy(upShape, new Vec3d(180, 0, 0)));
        return this;
    }

    public VoxelShaper withShape(VoxelShape shape, Direction facing) {
        shapes.put(facing, shape);
        return this;
    }

    public static Direction axisAsFace(Axis axis) {
        return Direction.get(AxisDirection.POSITIVE, axis);
    }

    protected static float horizontalAngleFromDirection(Direction direction) {
        return (float) ((Math.max(direction.getHorizontal(), 0) & 3) * 90);
    }

    protected static VoxelShaper forDirectionsWithRotation(VoxelShape shape, Direction facing,
                                                           Iterable<Direction> directions, Function<Direction, Vec3d> rotationValues) {
        VoxelShaper voxelShaper = new VoxelShaper();
        for (Direction dir : directions) {
            voxelShaper.shapes.put(dir, rotate(shape, facing, dir, rotationValues));
        }
        return voxelShaper;
    }

    protected static VoxelShape rotate(VoxelShape shape, Direction from, Direction to,
                                       Function<Direction, Vec3d> usingValues) {
        if (from == to)
            return shape;

        return rotatedCopy(shape, usingValues.apply(from)
            .negate()
            .add(usingValues.apply(to)));
    }

    protected static VoxelShape rotatedCopy(VoxelShape shape, Vec3d rotation) {
        if (rotation.equals(Vec3d.ZERO))
            return shape;

        MutableObject<VoxelShape> result = new MutableObject<>(VoxelShapes.empty());
        Vec3d center = new Vec3d(8, 8, 8);

        shape.forEachBox((x1, y1, z1, x2, y2, z2) -> {
            Vec3d v1 = new Vec3d(x1, y1, z1).multiply(16)
                .subtract(center);
            Vec3d v2 = new Vec3d(x2, y2, z2).multiply(16)
                .subtract(center);

            v1 = VecHelper.rotate(v1, (float) rotation.x, Axis.X);
            v1 = VecHelper.rotate(v1, (float) rotation.y, Axis.Y);
            v1 = VecHelper.rotate(v1, (float) rotation.z, Axis.Z)
                .add(center);

            v2 = VecHelper.rotate(v2, (float) rotation.x, Axis.X);
            v2 = VecHelper.rotate(v2, (float) rotation.y, Axis.Y);
            v2 = VecHelper.rotate(v2, (float) rotation.z, Axis.Z)
                .add(center);

            VoxelShape rotated = blockBox(v1, v2);
            result.setValue(VoxelShapes.union(result.getValue(), rotated));
        });

        return result.getValue();
    }

    protected static VoxelShape blockBox(Vec3d v1, Vec3d v2) {
        return createCuboidShape(
            Math.min(v1.x, v2.x),
            Math.min(v1.y, v2.y),
            Math.min(v1.z, v2.z),
            Math.max(v1.x, v2.x),
            Math.max(v1.y, v2.y),
            Math.max(v1.z, v2.z)
        );
    }

    protected static class DefaultRotationValues implements Function<Direction, Vec3d> {
        // assume facing up as the default rotation
        @Override
        public Vec3d apply(Direction direction) {
            return new Vec3d(direction == UP ? 0 : (Direction.Type.VERTICAL.test(direction) ? 180 : 90),
                -horizontalAngleFromDirection(direction), 0);
        }
    }

    protected static class HorizontalRotationValues implements Function<Direction, Vec3d> {
        @Override
        public Vec3d apply(Direction direction) {
            return new Vec3d(0, -horizontalAngleFromDirection(direction), 0);
        }
    }

    public static class Builder {

        private VoxelShape shape;

        public Builder(VoxelShape shape) {
            this.shape = shape;
        }

        public static Builder create(double x1, double y1, double z1, double x2, double y2, double z2) {
            return new Builder(createCuboidShape(x1, y1, z1, x2, y2, z2));
        }

        public Builder add(VoxelShape shape) {
            this.shape = VoxelShapes.union(this.shape, shape);
            return this;
        }

        public Builder add(double x1, double y1, double z1, double x2, double y2, double z2) {
            return add(createCuboidShape(x1, y1, z1, x2, y2, z2));
        }

        public Builder erase(double x1, double y1, double z1, double x2, double y2, double z2) {
            this.shape = VoxelShapes.combineAndSimplify(shape, createCuboidShape(x1, y1, z1, x2, y2, z2), BooleanBiFunction.ONLY_FIRST);
            return this;
        }

        public VoxelShape build() {
            return shape;
        }

        public VoxelShaper build(BiFunction<VoxelShape, Direction, VoxelShaper> factory, Direction direction) {
            return factory.apply(shape, direction);
        }

        public VoxelShaper build(BiFunction<VoxelShape, Axis, VoxelShaper> factory, Axis axis) {
            return factory.apply(shape, axis);
        }

        public VoxelShaper forDirectional(Direction direction) {
            return build(VoxelShaper::forDirectional, direction);
        }

        public VoxelShaper forAxis() {
            return build(VoxelShaper::forAxis, Axis.Y);
        }

        public VoxelShaper forHorizontalAxis() {
            return build(VoxelShaper::forHorizontalAxis, Axis.Z);
        }

        public VoxelShaper forHorizontal(Direction direction) {
            return build(VoxelShaper::forHorizontal, direction);
        }

        public VoxelShaper forDirectional() {
            return forDirectional(UP);
        }

    }

}

