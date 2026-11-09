package de.donutvisual;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.EquippableComponent;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.item.ItemStack;
import net.minecraft.item.equipment.EquipmentAssetKeys;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class RuleManager {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final Path FILE =
            FabricLoader.getInstance().getConfigDir().resolve("donutvisual").resolve("rules.json");

    public static final List<FakeRule> RULES = new ArrayList<>();

    public static void load() {
        try {
            if (Files.exists(FILE)) {
                try (Reader r = Files.newBufferedReader(FILE)) {
                    List<FakeRule> loaded = GSON.fromJson(r, new TypeToken<List<FakeRule>>() {}.getType());
                    RULES.clear();
                    if (loaded != null) RULES.addAll(loaded);
                }
            }
        } catch (Exception e) {
            DonutVisualClient.LOGGER.error("rules.json konnte nicht geladen werden", e);
        }
        FakeBlocks.rebuild();
    }

    public static void save() {
        try {
            Files.createDirectories(FILE.getParent());
            try (Writer w = Files.newBufferedWriter(FILE)) { GSON.toJson(RULES, w); }
        } catch (Exception e) {
            DonutVisualClient.LOGGER.error("rules.json konnte nicht gespeichert werden", e);
        }
        FakeBlocks.rebuild();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.worldRenderer != null && mc.world != null) mc.worldRenderer.reload(); // Chunks neu meshen
    }

    public static void addRule(FakeRule rule) {
        RULES.removeIf(r -> r.source.equals(rule.source));
        RULES.add(rule);
        save();
    }

    public static void apply(ItemStack stack) {
        if (stack.isEmpty()) return;
        String id = Registries.ITEM.getId(stack.getItem()).toString();

        for (FakeRule rule : RULES) {
            if (!rule.source.equals(id)) continue;
            Identifier target = Identifier.tryParse(rule.target);
            if (target == null) return;
            if (target.equals(stack.get(DataComponentTypes.ITEM_MODEL))) return;

            stack.set(DataComponentTypes.ITEM_MODEL, target);

            if (rule.name != null && !rule.name.isBlank()) {
                stack.set(DataComponentTypes.CUSTOM_NAME,
                        Text.literal(rule.name).setStyle(Style.EMPTY.withItalic(false)));
            }

            List<Text> lines = new ArrayList<>();
            for (String l : rule.lore) lines.add(Text.literal(l).setStyle(Style.EMPTY.withItalic(false)));
            if (rule.sellPrice >= 0) {
                lines.add(Text.literal("Sell Price: $" + formatPrice(rule.sellPrice))
                        .setStyle(Style.EMPTY.withItalic(false).withColor(Formatting.GREEN)));
            }
            if (!lines.isEmpty()) stack.set(DataComponentTypes.LORE, new LoreComponent(lines));

            // Elytra-Optik auf dem Ruecken: nur Equippable-Model, KEIN Glider-Component => kein Flug
            if (target.getPath().equals("elytra")) {
                RegistryKey key = RegistryKey.of(EquipmentAssetKeys.REGISTRY_KEY, Identifier.of("minecraft", "elytra"));
                stack.set(DataComponentTypes.EQUIPPABLE,
                        EquippableComponent.builder(EquipmentSlot.CHEST).model(key).build());
            }
            return;
        }
    }

    public static String formatPrice(double p) {
        if (p >= 1_000_000_000) return String.format("%.2fB", p / 1_000_000_000);
        if (p >= 1_000_000) return String.format("%.2fM", p / 1_000_000);
        if (p >= 1_000) return String.format("%.2fK", p / 1_000);
        return String.valueOf((long) p);
    }
}
