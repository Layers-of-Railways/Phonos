package io.github.foundationgames.phonos.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.mojang.datafixers.DataFixer;
import net.minecraft.datafixer.DataFixTypes;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.PersistentStateManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PersistentStateManager.class)
public class MixinPersistentStateManager {
    @WrapOperation(
        method = "readNbt",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/datafixer/DataFixTypes;update(Lcom/mojang/datafixers/DataFixer;Lnet/minecraft/nbt/NbtCompound;II)Lnet/minecraft/nbt/NbtCompound;"
        )
    )
    private NbtCompound ignoreNull(DataFixTypes instance, DataFixer dataFixer, NbtCompound nbt, int oldVersion, int newVersion, Operation<NbtCompound> original) {
        return instance == null ? nbt : original.call(instance, dataFixer, nbt, oldVersion, newVersion);
    }
}
