package de.donutvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.text.Text;

import java.util.List;

public class SchematicScreen extends Screen {
    private final Screen parent;
    private String selected = null;
    private String status = "Dateien nach config/donutvisual/schematics/ legen";

    public SchematicScreen(Screen parent) { super(Text.literal("Schematics")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = width / 2, y = 30;
        List<String> files = SchematicManager.list();
        for (int i = 0; i < Math.min(8, files.size()); i++) {
            final String f = files.get(i);
            addDrawableChild(ButtonWidget.builder(Text.literal(f), b -> { selected = f; status = "Ausgewaehlt: " + f; })
                    .dimensions(cx - 100, y + i * 22, 200, 20).build());
        }
        int by = y + 8 * 22 + 6;
        addDrawableChild(ButtonWidget.builder(Text.literal("Hier platzieren (meine Position)"), b -> {
            if (selected == null || client.player == null) { status = "Erst eine Datei waehlen"; return; }
            status = SchematicManager.place(selected, client.player.getBlockPos());
        }).dimensions(cx - 100, by, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Entfernen"), b -> { SchematicManager.clear(client); status = "Entfernt"; })
                .dimensions(cx - 100, by + 24, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> client.setScreen(parent))
                .dimensions(cx - 100, by + 48, 200, 20).build());
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, "Schematics (.litematic / .schem)", width / 2, 12, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 20, 0xFF55FF55);
    }
}
