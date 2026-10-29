package dev.npc;

import io.papermc.paper.command.brigadier.Commands;
import io.papermc.paper.plugin.lifecycle.event.types.LifecycleEvents;
import org.bukkit.plugin.java.JavaPlugin;

public final class NpcPlugin extends JavaPlugin {

    private NpcManager manager;

    @Override
    public void onEnable() {
        manager = new NpcManager(this);
        getServer().getPluginManager().registerEvents(new NpcListener(manager), this);

        getLifecycleManager().registerEventHandler(LifecycleEvents.COMMANDS, event -> {
            Commands registrar = event.registrar();
            registrar.register(NpcCommand.build(manager), "Управление NPC");
        });
    }

    @Override
    public void onDisable() {
        if (manager != null) manager.save();
    }
}
