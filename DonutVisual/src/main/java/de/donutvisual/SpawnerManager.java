package de.donutvisual;

import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.decoration.ArmorStandEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;

/** Fake-Spawner: lokales Stacking, Hologramm, lokales Menue. Nichts davon erreicht den Server. */
public class SpawnerManager {
    public static final Map<BlockPos, Integer> COUNTS = new HashMap<>();
    private static final Map<BlockPos, ArmorStandEntity> HOLOS = new HashMap<>();
    private static ClientWorld lastWorld;
    private static int nextId = -2_000_000;

    public static int count(BlockPos p) { return COUNTS.getOrDefault(p, 1); }

    public static void reset() { HOLOS.clear(); COUNTS.clear(); lastWorld = null; }

    private static String label(FakeRule r, BlockPos p) {
        String n = r.name == null || r.name.isBlank() ? "Spawner" : r.name;
        return "\u00a7e" + count(p) + "x \u00a7f" + n;
    }

    public static void tick(MinecraftClient c, int t) {
        if (c.world == null || c.player == null) { reset(); return; }
        if (c.world != lastWorld) { HOLOS.clear(); lastWorld = c.world; }
        if (t % 10 != 0) return;

        Set<BlockPos> seen = new HashSet<>();
        BlockPos p = c.player.getBlockPos();
        BlockPos.Mutable m = new BlockPos.Mutable();
        for (int x = -16; x <= 16; x++) for (int z = -16; z <= 16; z++) for (int y = -8; y <= 8; y++) {
            m.set(p.getX() + x, p.getY() + y, p.getZ() + z);
            BlockState st = c.world.getBlockState(m);
            FakeRule rule = FakeBlocks.ruleFor(st);
            if (rule == null || !FakeBlocks.isSpawner(rule)) continue;
            BlockPos pos = m.toImmutable();
            seen.add(pos);
            ArmorStandEntity e = HOLOS.get(pos);
            if (e == null) {
                e = new ArmorStandEntity(EntityType.ARMOR_STAND, c.world) {
                    @Override public boolean canHit() { return false; } // nicht anklickbar -> kein Server-Konflikt
                };
                e.setPosition(pos.getX() + 0.5, pos.getY() - 1.2, pos.getZ() + 0.5);
                e.setInvisible(true);
                e.setNoGravity(true);
                e.setCustomNameVisible(true);
                e.setId(nextId--);
                c.world.addEntity(e);
                HOLOS.put(pos, e);
            }
            e.setCustomName(Text.literal(label(rule, pos)));
        }
        Iterator<Map.Entry<BlockPos, ArmorStandEntity>> it = HOLOS.entrySet().iterator();
        while (it.hasNext()) {
            var en = it.next();
            if (!seen.contains(en.getKey())) {
                c.world.removeEntity(en.getValue().getId(), Entity.RemovalReason.DISCARDED);
                it.remove();
            }
        }
    }

    /** Rechtsklick auf Fake-Spawner: Stack (mit passendem Item) oder lokales Menue (leere Hand). Shift = normal platzieren. */
    public static ActionResult onUse(PlayerEntity player, World world, Hand hand, BlockHitResult hit) {
        if (!world.isClient() || hand != Hand.MAIN_HAND || player.isSneaking()) return ActionResult.PASS;
        BlockPos pos = hit.getBlockPos();
        FakeRule rule = FakeBlocks.ruleFor(world.getBlockState(pos));
        if (rule == null || !FakeBlocks.isSpawner(rule)) return ActionResult.PASS;

        ItemStack held = player.getMainHandStack();
        if (held.isEmpty()) {
            MinecraftClient.getInstance().setScreen(new SpawnerScreen(pos, rule));
            return ActionResult.FAIL; // FAIL = kein Paket an den Server
        }
        if (Registries.ITEM.getId(held.getItem()).toString().equals(rule.source)) {
            COUNTS.put(pos, count(pos) + 1);
            player.sendMessage(Text.literal("Stack: " + count(pos)), true);
            return ActionResult.FAIL;
        }
        return ActionResult.PASS;
    }
}
