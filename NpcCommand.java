package dev.npc;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.tree.LiteralCommandNode;
import io.papermc.paper.command.brigadier.CommandSourceStack;
import io.papermc.paper.command.brigadier.Commands;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public final class NpcCommand {

    private NpcCommand() {}

    public static LiteralCommandNode<CommandSourceStack> build(NpcManager mgr) {
        return Commands.literal("npc")
                .requires(src -> src.getSender().hasPermission("npc.admin"))

                // /npc create nickname <ник> [команда]
                // /npc create command <команда>
                .then(Commands.literal("create")
                        .then(Commands.literal("nickname")
                                .then(Commands.argument("nick", StringArgumentType.word())
                                        .executes(ctx -> create(ctx, mgr,
                                                StringArgumentType.getString(ctx, "nick"), null))
                                        .then(Commands.argument("command", StringArgumentType.greedyString())
                                                .executes(ctx -> create(ctx, mgr,
                                                        StringArgumentType.getString(ctx, "nick"),
                                                        StringArgumentType.getString(ctx, "command"))))))
                        .then(Commands.literal("command")
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> create(ctx, mgr, null,
                                                StringArgumentType.getString(ctx, "command"))))))

                // /npc remove <id>
                .then(Commands.literal("remove")
                        .then(idArg(mgr).executes(ctx -> {
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            return reply(ctx, mgr.remove(id), "NPC #" + id + " удалён.");
                        })))

                // /npc list
                .then(Commands.literal("list").executes(ctx -> {
                    var ids = mgr.ids();
                    CommandSender s = ctx.getSource().getSender();
                    if (ids.isEmpty()) {
                        s.sendMessage(Component.text("NPC нет.", NamedTextColor.YELLOW));
                    } else {
                        for (int id : ids) s.sendMessage(Component.text(mgr.describe(id), NamedTextColor.GRAY));
                    }
                    return Command.SINGLE_SUCCESS;
                }))

                // /npc tp <id>  (переместить NPC к себе)
                .then(Commands.literal("tp")
                        .then(idArg(mgr).executes(ctx -> {
                            if (!(ctx.getSource().getSender() instanceof Player p)) return onlyPlayer(ctx);
                            int id = IntegerArgumentType.getInteger(ctx, "id");
                            return reply(ctx, mgr.teleport(id, p.getLocation()), "NPC #" + id + " перемещён к вам.");
                        })))

                // /npc setcommand <id> <команда>
                .then(Commands.literal("setcommand")
                        .then(idArg(mgr)
                                .then(Commands.argument("command", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            int id = IntegerArgumentType.getInteger(ctx, "id");
                                            String cmd = StringArgumentType.getString(ctx, "command");
                                            return reply(ctx, mgr.setCommand(id, cmd), "Команда NPC #" + id + " изменена.");
                                        }))))

                // /npc setskin <id> <ник>
                .then(Commands.literal("setskin")
                        .then(idArg(mgr)
                                .then(Commands.argument("nick", StringArgumentType.word())
                                        .executes(ctx -> {
                                            int id = IntegerArgumentType.getInteger(ctx, "id");
                                            String nick = StringArgumentType.getString(ctx, "nick");
                                            return reply(ctx, mgr.setSkin(id, nick), "Скин NPC #" + id + " → " + nick);
                                        }))))

                // /npc rename <id> <имя в MiniMessage>
                .then(Commands.literal("rename")
                        .then(idArg(mgr)
                                .then(Commands.argument("name", StringArgumentType.greedyString())
                                        .executes(ctx -> {
                                            int id = IntegerArgumentType.getInteger(ctx, "id");
                                            String name = StringArgumentType.getString(ctx, "name");
                                            return reply(ctx, mgr.rename(id, name), "Имя NPC #" + id + " изменено.");
                                        }))))
                .build();
    }

    private static RequiredArgumentBuilder<CommandSourceStack, Integer> idArg(NpcManager mgr) {
        return Commands.argument("id", IntegerArgumentType.integer(1))
                .suggests((ctx, builder) -> {
                    mgr.ids().forEach(id -> builder.suggest(String.valueOf(id)));
                    return builder.buildFuture();
                });
    }

    private static int create(CommandContext<CommandSourceStack> ctx, NpcManager mgr, String nick, String command) {
        if (!(ctx.getSource().getSender() instanceof Player player)) return onlyPlayer(ctx);
        if (command != null && command.startsWith("/")) command = command.substring(1);

        int id = mgr.create(player.getLocation(), nick, command);
        player.sendMessage(Component.text("NPC #" + id + " создан"
                + (nick != null ? " (скин: " + nick + ")" : "")
                + (command != null ? ", команда: /" + command : "") + ".", NamedTextColor.GREEN));
        return Command.SINGLE_SUCCESS;
    }

    private static int reply(CommandContext<CommandSourceStack> ctx, boolean ok, String success) {
        ctx.getSource().getSender().sendMessage(ok
                ? Component.text(success, NamedTextColor.GREEN)
                : Component.text("NPC не найден (или чанк не загружен).", NamedTextColor.RED));
        return ok ? Command.SINGLE_SUCCESS : 0;
    }

    private static int onlyPlayer(CommandContext<CommandSourceStack> ctx) {
        ctx.getSource().getSender().sendMessage(Component.text("Только для игроков.", NamedTextColor.RED));
        return 0;
    }
}
