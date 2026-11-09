package de.donutvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

public class MainScreen extends Screen {
    public MainScreen() { super(Text.literal("DonutVisual")); }

    @Override
    protected void init() {
        int cx = width / 2, y = height / 2 - 55;
        btn("Regeln (Item -> Item)", cx, y, () -> client.setScreen(new RuleScreen(this)));
        btn("Presets (Spawner, Elytra, Netherite)", cx, y + 26, () -> client.setScreen(new PresetScreen(this)));
        btn("Schematics (Ghost-Basen)", cx, y + 52, () -> client.setScreen(new SchematicScreen(this)));
        btn("Alle Regeln loeschen", cx, y + 78, () -> { RuleManager.RULES.clear(); RuleManager.save(); });
        btn("Schliessen", cx, y + 104, this::close);
    }

    private void btn(String t, int cx, int y, Runnable r) {
        addDrawableChild(ButtonWidget.builder(Text.literal(t), b -> r.run()).dimensions(cx - 110, y, 220, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, "DonutVisual", width / 2, height / 2 - 80, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, "Regeln: " + RuleManager.RULES.size(), width / 2, height / 2 + 70, 0xFF888888);
    }
}
