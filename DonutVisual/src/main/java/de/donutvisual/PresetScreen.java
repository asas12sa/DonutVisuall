package de.donutvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

public class PresetScreen extends Screen {
    private static final String[] MOBS = {"Skeleton", "Zombie", "Piglin", "Blaze", "Spider", "Iron Golem", "Cow", "Pig", "Creeper"};
    private static final String[] GEAR = {"helmet", "chestplate", "leggings", "boots", "sword", "pickaxe", "axe", "shovel", "hoe"};

    private final Screen parent;
    private TextFieldWidget sourceField, priceField;
    private String status = "Quelle eintragen, dann Preset klicken";

    public PresetScreen(Screen parent) { super(Text.literal("Presets")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = width / 2, y = 30;
        sourceField = new TextFieldWidget(textRenderer, cx - 100, y, 200, 20, Text.literal("Quelle"));
        sourceField.setPlaceholder(Text.literal("Quelle, z.B. iron_block"));
        priceField = new TextFieldWidget(textRenderer, cx - 100, y + 24, 200, 20, Text.literal("Preis"));
        priceField.setPlaceholder(Text.literal("Sell-Preis (optional)"));
        addDrawableChild(sourceField); addDrawableChild(priceField);

        for (int i = 0; i < MOBS.length; i++) {
            final String mob = MOBS[i];
            addDrawableChild(ButtonWidget.builder(Text.literal(mob), b -> spawner(mob))
                    .dimensions(cx - 100 + (i % 3) * 68, y + 56 + (i / 3) * 24, 64, 20).build());
        }
        int by = y + 56 + 3 * 24 + 4;
        addDrawableChild(ButtonWidget.builder(Text.literal("Elytra (nur Optik)"), b -> elytra())
                .dimensions(cx - 100, by, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Netherite Gear (aus Diamond)"), b -> netherite())
                .dimensions(cx - 100, by + 24, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> client.setScreen(parent))
                .dimensions(cx - 100, by + 48, 200, 20).build());
    }

    private double price() {
        try { return priceField.getText().isBlank() ? -1 : Double.parseDouble(priceField.getText().replace(",", ".")); }
        catch (NumberFormatException e) { return -1; }
    }

    private String source() {
        String s = RuleScreen.normalize(sourceField.getText());
        Identifier id = Identifier.tryParse(s);
        if (id == null || !Registries.ITEM.containsId(id)) { status = "Unbekannte Quelle!"; return null; }
        return s;
    }

    private void spawner(String mob) {
        String src = source(); if (src == null) return;
        FakeRule r = new FakeRule(src, "minecraft:spawner", mob + " Spawner", price());
        r.mob = mob;
        RuleManager.addRule(r);
        status = src + " -> " + mob + " Spawner";
    }

    private void elytra() {
        String src = source(); if (src == null) return;
        RuleManager.addRule(new FakeRule(src, "minecraft:elytra", "Elytra", price()));
        status = src + " -> Elytra";
    }

    private void netherite() {
        for (String g : GEAR) {
            String name = "Netherite " + Character.toUpperCase(g.charAt(0)) + g.substring(1);
            RuleManager.addRule(new FakeRule("minecraft:diamond_" + g, "minecraft:netherite_" + g, name, -1));
        }
        status = "Diamond-Gear sieht jetzt wie Netherite aus";
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        ctx.drawCenteredTextWithShadow(textRenderer, "Presets", width / 2, 12, 0xFFFFFFFF);
        ctx.drawCenteredTextWithShadow(textRenderer, status, width / 2, height - 20, 0xFF55FF55);
    }
}
