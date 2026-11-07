package com.example.pearlwindchargeanalysis.mixin.client;

import net.minecraft.client.render.Camera;
import net.minecraft.client.render.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererFovInvoker {
    @Invoker("getFov")
    float pearlwind_chargeanalysis$invokeGetFov(
            Camera camera,
            float tickProgress,
            boolean changingFov
    );
}

