package de.donutvisual;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.screen.slot.Slot;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class DonutVisualClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger("DonutVisual");
    private static KeyBinding openMenu;
    private static int tick = 0;

    @Override
    public void onInitializeClient() {
        RuleManager.load();

        openMenu = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.donutvisual.open_menu", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.create(Identifier.of("donutvisual", "main"))));

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            while (openMenu.wasPressed()) client.setScreen(new MainScreen());
            tick++;
            applyRules(client);
            SpawnerManager.tick(client, tick);
            SchematicManager.tick(client, tick);
        });

        UseBlockCallback.EVENT.register(SpawnerManager::onUse);
        ClientReceiveMessageEvents.MODIFY_GAME.register(SellRewriter::rewrite);
        ClientPlayConnectionEvents.DISCONNECT.register((h, c) -> { SpawnerManager.reset(); SchematicManager.forget(); });
    }

    private static void applyRules(MinecraftClient client) {
        if (client.player == null || RuleManager.RULES.isEmpty()) return;
        PlayerInventory inv = client.player.getInventory();
        for (int i = 0; i < inv.size(); i++) RuleManager.apply(inv.getStack(i));
        // offene Container (Kisten, /sell-Menue ...)
        for (Slot s : client.player.currentScreenHandler.slots) RuleManager.apply(s.getStack());
    }
}
