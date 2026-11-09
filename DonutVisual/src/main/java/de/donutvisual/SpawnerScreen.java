package de.donutvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;
import net.minecraft.util.math.BlockPos;

public class SpawnerScreen extends Screen {
    private final BlockPos pos;
    private final FakeRule rule;

    public SpawnerScreen(BlockPos pos, FakeRule rule) {
        super(Text.literal("Spawner"));
        this.pos = pos; this.rule = rule;
    }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 20;
        addDrawableChild(ButtonWidget.builder(Text.literal("+1 Stack"),
                b -> SpawnerManager.COUNTS.put(pos, SpawnerManager.count(pos) + 1)).dimensions(cx - 100, y, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("-1 Stack"),
                b -> SpawnerManager.COUNTS.put(pos, Math.max(1, SpawnerManager.count(pos) - 1))).dimensions(cx + 5, y, 95, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Schliessen"), b -> close())
                .dimensions(cx - 100, y + 30, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        int cx = width / 2, y = height / 2 - 70;
        String n = rule.name == null || rule.name.isBlank() ? "Spawner" : rule.name;
        ctx.drawCenteredTextWithShadow(textRenderer, n, cx, y, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "Mob: " + (rule.mob.isBlank() ? "-" : rule.mob), cx, y + 14, 0xFFAAAAAA);
        ctx.drawCenteredTextWithShadow(textRenderer, "Stack: " + SpawnerManager.count(pos), cx, y + 28, 0xFFFFFF55);
        ctx.drawCenteredTextWithShadow(textRenderer, "(nur visuell)", cx, y + 42, 0xFF888888);
    }
}
