package io.github.foundationgames.phonos.mixin.client;

import dev.isxander.yacl3.gui.YACLScreen;
import dev.isxander.yacl3.gui.controllers.ControllerWidget;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.mixin_interfaces.YetAnotherConfigLibImplDuck;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ControllerWidget.class)
public class ControllerWidgetMixin {
    @Shadow @Final protected YACLScreen screen;

    @Inject(method = "isAvailable", at = @At("HEAD"), cancellable = true, remap = false)
    private void disableAvailabilityIfNotOp(CallbackInfoReturnable<Boolean> cir) {
        if (!(screen.config instanceof YetAnotherConfigLibImplDuck serverConfigLib)) return;
        if (!serverConfigLib.phonos$isServerSided()) return;

        var mc = MinecraftClient.getInstance();
        if (mc.player == null) {
            cir.setReturnValue(false);
            return;
        }

        if (!PhonosServerConfig.isAuthorizedToChange(mc.player)) {
            cir.setReturnValue(false);
        }
    }
}
