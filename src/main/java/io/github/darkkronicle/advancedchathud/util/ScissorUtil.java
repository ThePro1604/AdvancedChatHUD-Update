package io.github.darkkronicle.advancedchathud.util;

import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.gui.GuiGraphicsExtractor;

@Environment(EnvType.CLIENT)
public class ScissorUtil {

    private ScissorUtil() {
    }

    public static void applyScissorBox(GuiGraphicsExtractor drawContext, int x, int y, int width, int height) {
        drawContext.enableScissor(x, y, width + x, height + y);
    }

    public static void applyScissor(GuiGraphicsExtractor drawContext, int x1, int y1, int x2, int y2) {
        drawContext.enableScissor(x1, y1, x2 - x1, y2 - y1);
    }

    public static void resetScissor(GuiGraphicsExtractor drawContext) {
        drawContext.disableScissor();
    }
}
