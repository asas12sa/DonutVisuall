package de.donutvisual;

import java.util.ArrayList;
import java.util.List;

public class FakeRule {
    public String source = "minecraft:iron_block";
    public String target = "minecraft:spawner";
    public String name = "";
    public String mob = "";
    public List<String> lore = new ArrayList<>();
    public double sellPrice = -1;

    public FakeRule() {}

    public FakeRule(String source, String target, String name, double sellPrice) {
        this.source = source; this.target = target; this.name = name; this.sellPrice = sellPrice;
    }
}
