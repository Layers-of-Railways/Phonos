package io.github.foundationgames.phonos.mixin.nbs;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;

import java.io.DataOutputStream;
import java.io.IOException;

@Mixin(targets = "cz.koca2000.nbs4j.NBSWriter", remap = false)
public abstract class NBSWriterMixin {
    @Shadow
    private static void writeShort(DataOutputStream stream, short num) throws IOException {
        throw new AssertionError("Should be replaced by Mixin");
    }

    @WrapOperation(
        method = {
            "writeHeader",
            "writeMetadata"
        },
        at = @At(
            value = "INVOKE",
            target = "Ljava/io/DataOutputStream;writeShort(I)V"
        )
    )
    private static void fixEndianness(DataOutputStream instance, int v, Operation<Void> original) throws IOException {
        short s = (short) v;
        writeShort(instance, s);
    }
}
