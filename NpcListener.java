package dev.npc;

import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.persistence.PersistentDataType;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class NpcListener implements Listener {

    private final NpcManager manager;
    private final Map<UUID, Long> cooldown = new HashMap<>();

    public NpcListener(NpcManager manager) {
        this.manager = manager;
    }

    @EventHandler
    public void onInteract(PlayerInteractEntityEvent event) {
        if (!(event.getRightClicked() instanceof Mannequin npc)) return;
        if (!npc.getPersistentDataContainer().has(manager.idKey, PersistentDataType.INTEGER)) return;

        event.setCancelled(true);
        if (event.getHand() != EquipmentSlot.HAND) return;

        String command = npc.getPersistentDataContainer().get(manager.commandKey, PersistentDataType.STRING);
        if (command == null) return;

        Player player = event.getPlayer();
        long now = System.currentTimeMillis();
        if (now - cooldown.getOrDefault(player.getUniqueId(), 0L) < 500) return;
        cooldown.put(player.getUniqueId(), now);

        manager.runCommand(player, command);
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        cooldown.remove(event.getPlayer().getUniqueId());
    }
}
