package com.rideant.pearlwindchargeanalysisdebug;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.text.Text;

public final class DebugNoticeHud {
    private static final int WHITE = 0xFFFFFFFF;

    private DebugNoticeHud() {
    }

    public static void render(
            DrawContext context,
            RenderTickCounter tickCounter
    ) {
        MinecraftClient client = MinecraftClient.getInstance();
        String text = PearlWindChargeAnalysisDebugClient.getNoticeText();
        if (client.player == null
                || PearlWindChargeAnalysisDebugClient.getNoticeTicks() <= 0
                || text.isEmpty()) {
            return;
        }

        int width = client.textRenderer.getWidth(text);
        int x = (context.getScaledWindowWidth() - width) / 2;
        int y = context.getScaledWindowHeight() - 50;
        context.drawTextWithShadow(
                client.textRenderer,
                Text.literal(text),
                x,
                y,
                WHITE
        );
    }
}
