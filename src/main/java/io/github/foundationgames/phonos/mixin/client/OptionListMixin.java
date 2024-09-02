package io.github.foundationgames.phonos.mixin.client;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import dev.isxander.yacl3.api.ListOption;
import dev.isxander.yacl3.api.Option;
import dev.isxander.yacl3.gui.OptionListWidget;
import dev.isxander.yacl3.gui.YACLScreen;
import io.github.foundationgames.phonos.config.PhonosServerConfig;
import io.github.foundationgames.phonos.mixin_interfaces.YetAnotherConfigLibImplDuck;
import net.minecraft.client.MinecraftClient;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(OptionListWidget.class)
public class OptionListMixin {
    @Mixin(OptionListWidget.OptionEntry.class)
    private static class OptionEntryMixin {
        @WrapOperation(
            method = {
                "<init>",
                "lambda$new$1"
            },
            at = @At(
                value = "INVOKE",
                target = "Ldev/isxander/yacl3/api/Option;available()Z"
            ),
            remap = false
        )
        private boolean makeUnavailable(Option<?> instance, Operation<Boolean> original) {
            var mc = MinecraftClient.getInstance();
            var screen$ = mc.currentScreen;

            if (!(screen$ instanceof YACLScreen screen)) return original.call(instance);

            if (!(screen.config instanceof YetAnotherConfigLibImplDuck serverConfigLib)) return original.call(instance);
            if (!serverConfigLib.phonos$isServerSided()) return original.call(instance);

            if (mc.player == null)
                return false;

            if (!PhonosServerConfig.isAuthorizedToChange(mc.player))
                return false;

            return original.call(instance);
        }
    }

    @Mixin(OptionListWidget.ListGroupSeparatorEntry.class)
    private static class ListGroupSeparatorEntryMixin {
        @WrapOperation(
            method = "lambda$new$1",
            at = @At(
                value = "INVOKE",
                target = "Ldev/isxander/yacl3/api/Option;available()Z"
            ),
            remap = false
        )
        private boolean makeUnavailable(Option<?> instance, Operation<Boolean> original) {
            return phonos$makeUnavailable(instance, original);
        }

        @WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ldev/isxander/yacl3/api/ListOption;available()Z"))
        private boolean makeUnavailable(ListOption<?> instance, Operation<Boolean> original) {
            return phonos$makeUnavailable(instance, original);
        }

        @Unique
        private static boolean phonos$makeUnavailable(Option<?> instance, Operation<Boolean> original) {
            var mc = MinecraftClient.getInstance();
            var screen$ = mc.currentScreen;

            if (!(screen$ instanceof YACLScreen screen)) return original.call(instance);

            if (!(screen.config instanceof YetAnotherConfigLibImplDuck serverConfigLib)) return original.call(instance);
            if (!serverConfigLib.phonos$isServerSided()) return original.call(instance);

            if (mc.player == null)
                return false;

            if (!PhonosServerConfig.isAuthorizedToChange(mc.player))
                return false;

            return original.call(instance);
        }
    }
}
