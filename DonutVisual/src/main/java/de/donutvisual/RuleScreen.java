package de.donutvisual;

import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.widget.ButtonWidget;
import net.minecraft.client.gui.widget.TextFieldWidget;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

public class RuleScreen extends Screen {
    private final Screen parent;
    private TextFieldWidget sourceField, targetField, nameField, priceField, suggestionFor;
    private List<String> suggestions = new ArrayList<>();
    private String status = "";

    public RuleScreen(Screen parent) { super(Text.literal("Regeln")); this.parent = parent; }

    @Override
    protected void init() {
        int cx = width / 2, y = 40;
        sourceField = field(cx - 100, y, "Quelle, z.B. iron_block");
        targetField = field(cx - 100, y + 32, "Ziel, z.B. spawner");
        nameField   = field(cx - 100, y + 64, "Anzeigename (optional)");
        priceField  = field(cx - 100, y + 96, "Sell-Preis (optional)");
        sourceField.setChangedListener(s -> updateSuggestions(sourceField));
        targetField.setChangedListener(s -> updateSuggestions(targetField));

        addDrawableChild(ButtonWidget.builder(Text.literal("Regel hinzufuegen"), b -> addRule())
                .dimensions(cx - 100, y + 128, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Letzte Regel loeschen"), b -> {
            if (!RuleManager.RULES.isEmpty()) { RuleManager.RULES.remove(RuleManager.RULES.size() - 1); RuleManager.save(); }
        }).dimensions(cx - 100, y + 152, 200, 20).build());
        addDrawableChild(ButtonWidget.builder(Text.literal("Zurueck"), b -> client.setScreen(parent))
                .dimensions(cx - 100, y + 176, 200, 20).build());
    }

    private TextFieldWidget field(int x, int y, String ph) {
        TextFieldWidget f = new TextFieldWidget(textRenderer, x, y, 200, 20, Text.literal(ph));
        f.setPlaceholder(Text.literal(ph));
        f.setMaxLength(128);
        addDrawableChild(f);
        return f;
    }

    private void updateSuggestions(TextFieldWidget f) {
        suggestionFor = f;
        String q = f.getText().toLowerCase().replace("minecraft:", "");
        suggestions = new ArrayList<>();
        if (q.length() < 2) return;
        for (Identifier id : Registries.ITEM.getIds()) {
            if (id.getPath().contains(q)) suggestions.add(id.toString());
            if (suggestions.size() >= 6) break;
        }
    }

    static String normalize(String s) {
        s = s.trim().toLowerCase();
        return s.contains(":") ? s : "minecraft:" + s;
    }

    private void addRule() {
        String src = normalize(sourceField.getText()), tgt = normalize(targetField.getText());
        Identifier s = Identifier.tryParse(src), t = Identifier.tryParse(tgt);
        if (s == null || t == null || !Registries.ITEM.containsId(s) || !Registries.ITEM.containsId(t)) {
            status = "Unbekanntes Item!"; return;
        }
        double price = -1;
        if (!priceField.getText().isBlank()) {
            try { price = Double.parseDouble(priceField.getText().replace(",", ".")); }
            catch (NumberFormatException e) { status = "Preis ungueltig"; return; }
        }
        FakeRule r = new FakeRule(src, tgt, nameField.getText(), price);
        if (tgt.equals("minecraft:spawner")) r.mob = nameField.getText().replace(" Spawner", "");
        RuleManager.addRule(r);
        status = "Gespeichert: " + src + " -> " + tgt;
    }

    @Override
    public void render(DrawContext ctx, int mx, int my, float d) {
        super.render(ctx, mx, my, d);
        int cx = width / 2;
        ctx.drawCenteredTextWithShadow(textRenderer, "Regeln", cx, 15, 0xFFFFFFFF);
        if (suggestionFor != null && suggestionFor.isFocused()) {
            int sy = suggestionFor.getY() + 22;
            for (String s : suggestions) { ctx.drawTextWithShadow(textRenderer, s, suggestionFor.getX() + 4, sy, 0xFFAAAAAA); sy += 10; }
        }
        ctx.drawTextWithShadow(textRenderer, status, cx - 100, 240, 0xFF55FF55);
        int ry = 255;
        ctx.drawTextWithShadow(textRenderer, "Aktive Regeln: " + RuleManager.RULES.size(), cx - 100, ry, 0xFFFFFFFF);
        for (int i = 0; i < Math.min(6, RuleManager.RULES.size()); i++) {
            FakeRule r = RuleManager.RULES.get(i);
            ctx.drawTextWithShadow(textRenderer, r.source + " -> " + r.target, cx - 100, ry + 12 + i * 10, 0xFFCCCCCC);
        }
    }
}
