package dev.npc;

import io.papermc.paper.datacomponent.item.ResolvableProfile;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.NamespacedKey;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Mannequin;
import org.bukkit.entity.Player;
import org.bukkit.persistence.PersistentDataType;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public final class NpcManager {

    private final NpcPlugin plugin;
    private final File file;
    private final YamlConfiguration data;

    public final NamespacedKey commandKey;
    public final NamespacedKey idKey;

    public NpcManager(NpcPlugin plugin) {
        this.plugin = plugin;
        this.commandKey = new NamespacedKey(plugin, "command");
        this.idKey = new NamespacedKey(plugin, "id");
        plugin.getDataFolder().mkdirs();
        this.file = new File(plugin.getDataFolder(), "data.yml");
        this.data = YamlConfiguration.loadConfiguration(file);
    }

    // ---------- создание ----------

    /** @param skin ник для скина (может быть null), @param command команда по клику (может быть null) */
    public int create(Location loc, String skin, String command) {
        int id = data.getInt("next-id", 1);
        data.set("next-id", id + 1);

        Mannequin npc = loc.getWorld().spawn(loc, Mannequin.class, m -> {
            m.setPersistent(true);
            m.setInvulnerable(true);
            m.setImmovable(true);
            m.setGravity(false);
            m.setSilent(true);
            m.setDescription(null); // убираем надпись "NPC" под ником
            m.getPersistentDataContainer().set(idKey, PersistentDataType.INTEGER, id);

            if (skin != null) {
                m.setProfile(ResolvableProfile.resolvableProfile().name(skin).build());
                m.customName(Component.text(skin));
                m.setCustomNameVisible(true);
            }
            if (command != null) {
                m.getPersistentDataContainer().set(commandKey, PersistentDataType.STRING, command);
            }
        });

        String path = "npcs." + id;
        data.set(path + ".uuid", npc.getUniqueId().toString());
        data.set(path + ".skin", skin);
        writeLocation(path, loc);
        save();
        return id;
    }

    // ---------- поиск ----------

    public Mannequin find(int id) {
        ConfigurationSection s = data.getConfigurationSection("npcs." + id);
        if (s == null) return null;
        World w = Bukkit.getWorld(s.getString("world", ""));
        if (w == null) return null;
        // прогружаем чанк, чтобы сущность была доступна
        w.getChunkAt((int) Math.floor(s.getDouble("x")) >> 4, (int) Math.floor(s.getDouble("z")) >> 4);
        var e = Bukkit.getEntity(UUID.fromString(s.getString("uuid", "")));
        return e instanceof Mannequin m ? m : null;
    }

    public boolean exists(int id) {
        return data.contains("npcs." + id);
    }

    public List<Integer> ids() {
        List<Integer> list = new ArrayList<>();
        ConfigurationSection s = data.getConfigurationSection("npcs");
        if (s != null) for (String k : s.getKeys(false)) list.add(Integer.parseInt(k));
        return list;
    }

    public String describe(int id) {
        ConfigurationSection s = data.getConfigurationSection("npcs." + id);
        if (s == null) return "?";
        Mannequin m = find(id);
        String cmd = m == null ? "-" : m.getPersistentDataContainer().get(commandKey, PersistentDataType.STRING);
        return "#" + id
                + " skin=" + (s.getString("skin") == null ? "-" : s.getString("skin"))
                + " cmd=" + (cmd == null ? "-" : "/" + cmd)
                + " @ " + s.getString("world") + " "
                + (int) s.getDouble("x") + " " + (int) s.getDouble("y") + " " + (int) s.getDouble("z");
    }

    // ---------- изменение ----------

    public boolean remove(int id) {
        if (!exists(id)) return false;
        Mannequin m = find(id);
        if (m != null) m.remove();
        data.set("npcs." + id, null);
        save();
        return true;
    }

    public boolean setCommand(int id, String command) {
        Mannequin m = find(id);
        if (m == null) return false;
        m.getPersistentDataContainer().set(commandKey, PersistentDataType.STRING, command);
        return true;
    }

    public boolean setSkin(int id, String nick) {
        Mannequin m = find(id);
        if (m == null) return false;
        m.setProfile(ResolvableProfile.resolvableProfile().name(nick).build());
        data.set("npcs." + id + ".skin", nick);
        save();
        return true;
    }

    public boolean rename(int id, String miniMessage) {
        Mannequin m = find(id);
        if (m == null) return false;
        m.customName(MiniMessage.miniMessage().deserialize(miniMessage));
        m.setCustomNameVisible(true);
        return true;
    }

    public boolean teleport(int id, Location loc) {
        Mannequin m = find(id);
        if (m == null) return false;
        m.teleport(loc);
        writeLocation("npcs." + id, loc);
        save();
        return true;
    }

    // ---------- клик ----------

    public void runCommand(Player player, String command) {
        String cmd = command.replace("{player}", player.getName());
        if (cmd.startsWith("/")) cmd = cmd.substring(1);
        if (cmd.regionMatches(true, 0, "console:", 0, 8)) {
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), cmd.substring(8).trim());
        } else {
            player.performCommand(cmd);
        }
    }

    // ---------- хранение ----------

    private void writeLocation(String path, Location l) {
        data.set(path + ".world", l.getWorld().getName());
        data.set(path + ".x", l.getX());
        data.set(path + ".y", l.getY());
        data.set(path + ".z", l.getZ());
    }

    public void save() {
        try {
            data.save(file);
        } catch (IOException e) {
            plugin.getLogger().severe("Не удалось сохранить data.yml: " + e.getMessage());
        }
    }
}
