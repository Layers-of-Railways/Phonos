package io.github.foundationgames.phonos.util;

import net.minecraft.util.math.Direction.Axis;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

// Copied from the Create mod, under the MIT license
public class VecHelper {
    public static Vec3d rotate(Vec3d vec, double deg, Axis axis) {
        if (deg == 0)
            return vec;
        if (vec == Vec3d.ZERO)
            return vec;

        float angle = (float) (deg / 180f * Math.PI);
        double sin = MathHelper.sin(angle);
        double cos = MathHelper.cos(angle);
        double x = vec.x;
        double y = vec.y;
        double z = vec.z;

        if (axis == Axis.X)
            return new Vec3d(x, y * cos - z * sin, z * cos + y * sin);
        if (axis == Axis.Y)
            return new Vec3d(x * cos + z * sin, y, z * cos - x * sin);
        if (axis == Axis.Z)
            return new Vec3d(x * cos - y * sin, y * cos + x * sin, z);
        return vec;
    }
}
