package de.donutvisual;

import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.nbt.*;
import net.minecraft.registry.Registries;
import net.minecraft.util.math.BlockPos;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Stream;

/** Ghost-Basen: setzt Bloecke nur im ClientWorld-Chunk-Cache (Server weiss nichts davon). */
public class SchematicManager {
    public static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("donutvisual").resolve("schematics");
    private static final Map<BlockPos, BlockState> GHOST = new HashMap<>();
    private static final Map<BlockPos, BlockState> ORIG = new HashMap<>();

    record Entry(int x, int y, int z, BlockState state) {}

    public static List<String> list() {
        List<String> out = new ArrayList<>();
        try {
            Files.createDirectories(DIR);
            try (Stream<Path> s = Files.list(DIR)) {
                s.map(p -> p.getFileName().toString())
                 .filter(n -> n.endsWith(".litematic") || n.endsWith(".schem"))
                 .sorted().forEach(out::add);
            }
        } catch (Exception ignored) {}
        return out;
    }

    public static String place(String file, BlockPos origin) {
        try {
            NbtCompound root = NbtIo.readCompressed(DIR.resolve(file), NbtSizeTracker.ofUnlimitedBytes());
            List<Entry> entries = file.endsWith(".litematic") ? loadLitematic(root) : loadSponge(root);
            clear(MinecraftClient.getInstance());
            for (Entry e : entries) {
                if (e.state.hasBlockEntity()) continue;
                GHOST.put(origin.add(e.x, e.y, e.z), e.state);
            }
            return GHOST.size() + " Bloecke geladen";
        } catch (Exception ex) {
            DonutVisualClient.LOGGER.error("Schematic-Fehler", ex);
            return "Fehler: " + ex.getMessage();
        }
    }

    public static void clear(MinecraftClient c) {
        if (c != null && c.world != null) {
            for (var e : ORIG.entrySet()) c.world.setBlockState(e.getKey(), e.getValue(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
        }
        GHOST.clear(); ORIG.clear();
    }

    public static void forget() { GHOST.clear(); ORIG.clear(); }

    public static void tick(MinecraftClient c, int t) {
        if (GHOST.isEmpty() || c.world == null || c.player == null || t % 20 != 0) return;
        ClientWorld w = c.world;
        BlockPos p = c.player.getBlockPos();
        int budget = 6000;
        for (var e : GHOST.entrySet()) {
            BlockPos pos = e.getKey();
            if (Math.abs(pos.getX() - p.getX()) > 96 || Math.abs(pos.getZ() - p.getZ()) > 96) continue;
            BlockState cur = w.getBlockState(pos);
            if (cur.equals(e.getValue())) continue;
            ORIG.putIfAbsent(pos, cur);
            w.setBlockState(pos, e.getValue(), Block.NOTIFY_LISTENERS | Block.FORCE_STATE);
            if (--budget <= 0) break;
        }
    }

    // ---------- Litematica ----------
    private static List<Entry> loadLitematic(NbtCompound root) {
        NbtCompound regions = root.getCompoundOrEmpty("Regions");
        List<Entry> all = new ArrayList<>();
        int minX = Integer.MAX_VALUE, minY = Integer.MAX_VALUE, minZ = Integer.MAX_VALUE;

        for (String key : regions.getKeys()) {
            NbtCompound reg = regions.getCompoundOrEmpty(key);
            NbtCompound pos = reg.getCompoundOrEmpty("Position"), size = reg.getCompoundOrEmpty("Size");
            int sx = size.getInt("x", 0), sy = size.getInt("y", 0), sz = size.getInt("z", 0);
            int bx = pos.getInt("x", 0) + (sx < 0 ? sx + 1 : 0);
            int by = pos.getInt("y", 0) + (sy < 0 ? sy + 1 : 0);
            int bz = pos.getInt("z", 0) + (sz < 0 ? sz + 1 : 0);
            int ax = Math.abs(sx), ay = Math.abs(sy), az = Math.abs(sz);

            NbtList pal = reg.getListOrEmpty("BlockStatePalette");
            BlockState[] states = new BlockState[pal.size()];
            for (int i = 0; i < states.length; i++)
                states[i] = net.minecraft.nbt.NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), pal.getCompoundOrEmpty(i));

            long[] arr = reg.getLongArray("BlockStates").orElse(new long[0]);
            int bits = Math.max(2, 32 - Integer.numberOfLeadingZeros(Math.max(1, states.length - 1)));
            long mask = (1L << bits) - 1;

            for (int y = 0; y < ay; y++) for (int z = 0; z < az; z++) for (int x = 0; x < ax; x++) {
                long idx = ((long) y * az + z) * ax + x;
                long bit = idx * bits;
                int li = (int) (bit >>> 6), off = (int) (bit & 63);
                if (li >= arr.length) continue;
                long v = arr[li] >>> off;
                if (off + bits > 64 && li + 1 < arr.length) v |= arr[li + 1] << (64 - off);
                int id = (int) (v & mask);
                if (id >= states.length || states[id].isAir()) continue;
                int wx = bx + x, wy = by + y, wz = bz + z;
                minX = Math.min(minX, wx); minY = Math.min(minY, wy); minZ = Math.min(minZ, wz);
                all.add(new Entry(wx, wy, wz, states[id]));
            }
        }
        List<Entry> out = new ArrayList<>(all.size());
        for (Entry e : all) out.add(new Entry(e.x - minX, e.y - minY, e.z - minZ, e.state));
        return out;
    }

    // ---------- Sponge .schem (v2 + v3) ----------
    private static List<Entry> loadSponge(NbtCompound root) {
        NbtCompound sch = root.contains("Schematic") ? root.getCompoundOrEmpty("Schematic") : root;
        int w = sch.getShort("Width", (short) 0) & 0xFFFF;
        int h = sch.getShort("Height", (short) 0) & 0xFFFF;
        int l = sch.getShort("Length", (short) 0) & 0xFFFF;

        NbtCompound palNbt; byte[] data;
        if (sch.contains("Blocks")) {
            NbtCompound b = sch.getCompoundOrEmpty("Blocks");
            palNbt = b.getCompoundOrEmpty("Palette");
            data = b.getByteArray("Data").orElse(new byte[0]);
        } else {
            palNbt = sch.getCompoundOrEmpty("Palette");
            data = sch.getByteArray("BlockData").orElse(new byte[0]);
        }
        Map<Integer, BlockState> pal = new HashMap<>();
        for (String k : palNbt.getKeys()) pal.put(palNbt.getInt(k, 0), parseState(k));

        List<Entry> out = new ArrayList<>();
        int pos = 0, i = 0;
        while (pos < data.length && i < w * h * l) {
            int val = 0, shift = 0;
            while (pos < data.length) {
                int b = data[pos++];
                val |= (b & 0x7F) << shift;
                if ((b & 0x80) == 0) break;
                shift += 7;
            }
            BlockState st = pal.get(val);
            if (st != null && !st.isAir()) {
                int x = i % w, z = (i / w) % l, y = i / (w * l);
                out.add(new Entry(x, y, z, st));
            }
            i++;
        }
        return out;
    }

    private static BlockState parseState(String s) {
        NbtCompound c = new NbtCompound();
        int br = s.indexOf('[');
        c.putString("Name", br < 0 ? s : s.substring(0, br));
        if (br >= 0) {
            NbtCompound props = new NbtCompound();
            for (String kv : s.substring(br + 1, s.length() - 1).split(",")) {
                String[] a = kv.split("=");
                if (a.length == 2) props.putString(a[0], a[1]);
            }
            c.put("Properties", props);
        }
        return net.minecraft.nbt.NbtHelper.toBlockState(Registries.BLOCK.getReadOnlyWrapper(), c);
    }
}
