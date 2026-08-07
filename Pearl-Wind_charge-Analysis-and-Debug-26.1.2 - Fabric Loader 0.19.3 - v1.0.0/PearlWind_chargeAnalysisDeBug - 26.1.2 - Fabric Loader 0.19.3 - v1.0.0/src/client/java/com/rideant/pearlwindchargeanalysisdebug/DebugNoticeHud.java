package com.rideant.pearlwindchargeanalysisdebug;

import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.network.chat.Component;

public final class DebugNoticeHud {
    private static final int WHITE = 0xFFFFFFFF;

    private DebugNoticeHud() {
    }

    public static void render(
            GuiGraphicsExtractor context,
            DeltaTracker tickCounter
    ) {
        Minecraft client = Minecraft.getInstance();
        String text = PearlWindChargeAnalysisDebugClient.getNoticeText();
        if (client.player == null
                || PearlWindChargeAnalysisDebugClient.getNoticeTicks() <= 0
                || text.isEmpty()) {
            return;
        }

        int width = client.font.width(text);
        int x = (context.guiWidth() - width) / 2;
        int y = context.guiHeight() - 28;
        context.text(
                client.font,
                Component.literal(text),
                x,
                y,
                WHITE
        );
    }
}
