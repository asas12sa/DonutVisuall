package de.donutvisual;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/** Block-Mapping fuer die Welt-Optik (wird aus den Mixins gelesen, daher volatile + immutable). */
public class FakeBlocks {
    private static volatile Map<Block, BlockState> STATES = Map.of();
    private static volatile Map<Block, FakeRule> RULES = Map.of();

    public static void rebuild() {
        Map<Block, BlockState> states = new HashMap<>();
        Map<Block, FakeRule> rules = new HashMap<>();
        for (FakeRule r : RuleManager.RULES) {
            Identifier s = Identifier.tryParse(r.source), t = Identifier.tryParse(r.target);
            if (s == null || t == null || !Registries.BLOCK.containsId(s) || !Registries.BLOCK.containsId(t)) continue;
            Block sb = Registries.BLOCK.get(s), tb = Registries.BLOCK.get(t);
            if (sb == tb) continue;
            states.put(sb, tb.getDefaultState());
            rules.put(sb, r);
        }
        // Zyklen verhindern (A->B und B->A)
        states.entrySet().removeIf(e -> states.containsKey(e.getValue().getBlock()));
        rules.keySet().retainAll(states.keySet());
        STATES = Map.copyOf(states);
        RULES = Map.copyOf(rules);
    }

    public static BlockState targetState(BlockState s) { return STATES.get(s.getBlock()); }
    public static FakeRule ruleFor(BlockState s) { return RULES.get(s.getBlock()); }
    public static boolean isSpawner(FakeRule r) { return r.target.equals("minecraft:spawner"); }
}
