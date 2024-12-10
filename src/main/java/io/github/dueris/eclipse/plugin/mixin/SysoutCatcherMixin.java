package io.github.dueris.eclipse.plugin.mixin;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import io.papermc.paper.logging.SysoutCatcher;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

import java.io.PrintStream;

@Mixin(SysoutCatcher.class)
public class SysoutCatcherMixin {

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/System;setOut(Ljava/io/PrintStream;)V"))
	public void eclipse$no(PrintStream out, Operation<Void> original) {
	}

	@WrapOperation(method = "<init>", at = @At(value = "INVOKE", target = "Ljava/lang/System;setErr(Ljava/io/PrintStream;)V"))
	public void eclipse$alsoNo(PrintStream err, Operation<Void> original) {
	}
}
