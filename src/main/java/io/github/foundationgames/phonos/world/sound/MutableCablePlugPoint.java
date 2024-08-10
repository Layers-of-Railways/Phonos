package io.github.foundationgames.phonos.world.sound;

import io.github.foundationgames.phonos.util.Pose3f;
import net.minecraft.world.World;

public class MutableCablePlugPoint implements CablePlugPoint {

    private Pose3f pose;
    private Pose3f originPose;

    public MutableCablePlugPoint(Pose3f pose, Pose3f originPose) {
        this.pose = pose;
        this.originPose = originPose;
    }

    @Override
    public void writePlugPose(World world, float delta, Pose3f out) {
        out.set(pose);
    }

    @Override
    public void writeOriginPose(World world, float delta, Pose3f out) {
        out.set(originPose);
    }

    @Override
    public boolean isStatic() {
        return false;
    }

    public void setPose(Pose3f pose) {
        this.pose = pose;
    }

    public void setOriginPose(Pose3f originPose) {
        this.originPose = originPose;
    }
}
