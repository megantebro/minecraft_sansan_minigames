package lobby.minecraft_minigames.command;

import lobby.minecraft_minigames.Minecraft_minigames;
import lobby.minecraft_minigames.manager.PartyManager;
import lobby.minecraft_minigames.model.Party;
import lobby.minecraft_minigames.util.MessageUtil;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import java.util.*;
import java.util.stream.Collectors;

public class PartyCommand implements CommandExecutor, TabCompleter {

    private final Minecraft_minigames plugin;
    private final PartyManager partyManager;

    public PartyCommand(Minecraft_minigames plugin, PartyManager partyManager) {
        this.plugin = plugin;
        this.partyManager = partyManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player)) {
            sender.sendMessage("This command can only be used by players.");
            return true;
        }

        if (args.length == 0) {
            sendHelp(player);
            return true;
        }

        switch (args[0].toLowerCase()) {
            case "create" -> handleCreate(player);
            case "invite" -> handleInvite(player, args);
            case "accept" -> handleAccept(player, args);
            case "deny" -> handleDeny(player, args);
            case "leave" -> handleLeave(player);
            case "kick" -> handleKick(player, args);
            case "list" -> handleList(player);
            case "disband" -> handleDisband(player);
            default -> sendHelp(player);
        }

        return true;
    }

    private void handleCreate(Player player) {
        if (partyManager.isInParty(player.getUniqueId())) {
            MessageUtil.send(player, "&c既にパーティーに参加しています。");
            return;
        }
        Party party = partyManager.createParty(player.getUniqueId());
        if (party != null) {
            MessageUtil.send(player, "&aパーティーを作成しました！ &7/party invite <player> で招待できます。");
        }
    }

    private void handleInvite(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, "&c使い方: /party invite <player>");
            return;
        }
        if (!partyManager.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cパーティーリーダーのみ招待できます。");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, "&cプレイヤー '" + args[1] + "' が見つかりません。");
            return;
        }
        if (target.getUniqueId().equals(player.getUniqueId())) {
            MessageUtil.send(player, "&c自分自身を招待できません。");
            return;
        }
        if (partyManager.invitePlayer(player.getUniqueId(), target.getUniqueId())) {
            MessageUtil.send(player, "&a" + target.getName() + " &fを招待しました。");
            MessageUtil.send(target, "&a" + player.getName() + " &fからパーティーの招待が届きました！");
            MessageUtil.sendNoPrefix(target, "&e/party accept &7で参加 | &e/party deny &7で拒否");
        } else {
            MessageUtil.send(player, "&c招待できませんでした。パーティーが満員か、相手が既にパーティーに参加しています。");
        }
    }

    private void handleAccept(Player player, String[] args) {
        UUID inviteFrom = partyManager.findPartyWithInvite(player.getUniqueId());
        if (inviteFrom == null) {
            MessageUtil.send(player, "&c招待がありません。");
            return;
        }
        if (partyManager.acceptInvite(player.getUniqueId(), inviteFrom)) {
            MessageUtil.send(player, "&aパーティーに参加しました！");
            Player leader = Bukkit.getPlayer(inviteFrom);
            if (leader != null) {
                MessageUtil.send(leader, "&a" + player.getName() + " &fがパーティーに参加しました。");
            }
        } else {
            MessageUtil.send(player, "&cパーティーへの参加に失敗しました。");
        }
    }

    private void handleDeny(Player player, String[] args) {
        UUID inviteFrom = partyManager.findPartyWithInvite(player.getUniqueId());
        if (inviteFrom == null) {
            MessageUtil.send(player, "&c招待がありません。");
            return;
        }
        partyManager.denyInvite(player.getUniqueId(), inviteFrom);
        MessageUtil.send(player, "&7招待を拒否しました。");
    }

    private void handleLeave(Player player) {
        if (!partyManager.isInParty(player.getUniqueId())) {
            MessageUtil.send(player, "&cパーティーに参加していません。");
            return;
        }
        partyManager.leaveParty(player.getUniqueId());
        MessageUtil.send(player, "&7パーティーから退出しました。");
    }

    private void handleKick(Player player, String[] args) {
        if (args.length < 2) {
            MessageUtil.send(player, "&c使い方: /party kick <player>");
            return;
        }
        if (!partyManager.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cパーティーリーダーのみキックできます。");
            return;
        }
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            MessageUtil.send(player, "&cプレイヤー '" + args[1] + "' が見つかりません。");
            return;
        }
        partyManager.kickPlayer(player.getUniqueId(), target.getUniqueId());
        MessageUtil.send(player, "&a" + target.getName() + " &fをキックしました。");
        MessageUtil.send(target, "&cパーティーからキックされました。");
    }

    private void handleList(Player player) {
        Party party = partyManager.getParty(player.getUniqueId());
        if (party == null) {
            MessageUtil.send(player, "&cパーティーに参加していません。");
            return;
        }
        MessageUtil.send(player, "&6=== パーティーメンバー ===");
        for (UUID member : party.getMembers()) {
            Player p = Bukkit.getPlayer(member);
            String name = p != null ? p.getName() : member.toString();
            String role = party.isLeader(member) ? " &c[リーダー]" : "";
            MessageUtil.sendNoPrefix(player, "&8 - &f" + name + role);
        }
    }

    private void handleDisband(Player player) {
        if (!partyManager.isLeader(player.getUniqueId())) {
            MessageUtil.send(player, "&cパーティーリーダーのみ解散できます。");
            return;
        }
        Party party = partyManager.getParty(player.getUniqueId());
        if (party != null) {
            for (UUID member : party.getMembers()) {
                Player p = Bukkit.getPlayer(member);
                if (p != null) {
                    MessageUtil.send(p, "&cパーティーが解散されました。");
                }
            }
        }
        partyManager.disbandParty(player.getUniqueId());
    }

    private void sendHelp(Player player) {
        MessageUtil.send(player, "&6=== パーティー ヘルプ ===");
        MessageUtil.sendNoPrefix(player, "&e/party create &7- パーティーを作成");
        MessageUtil.sendNoPrefix(player, "&e/party invite <player> &7- プレイヤーを招待");
        MessageUtil.sendNoPrefix(player, "&e/party accept &7- 招待を承認");
        MessageUtil.sendNoPrefix(player, "&e/party deny &7- 招待を拒否");
        MessageUtil.sendNoPrefix(player, "&e/party leave &7- パーティーから退出");
        MessageUtil.sendNoPrefix(player, "&e/party kick <player> &7- プレイヤーをキック");
        MessageUtil.sendNoPrefix(player, "&e/party list &7- メンバー一覧");
        MessageUtil.sendNoPrefix(player, "&e/party disband &7- パーティーを解散");
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String label, String[] args) {
        if (args.length == 1) {
            return filterStartsWith(
                    List.of("create", "invite", "accept", "deny", "leave", "kick", "list", "disband"),
                    args[0]
            );
        }
        if (args.length == 2 && (args[0].equalsIgnoreCase("invite") || args[0].equalsIgnoreCase("kick"))) {
            return filterStartsWith(
                    Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList()),
                    args[1]
            );
        }
        return Collections.emptyList();
    }

    private List<String> filterStartsWith(List<String> options, String input) {
        String lower = input.toLowerCase();
        return options.stream().filter(s -> s.toLowerCase().startsWith(lower)).collect(Collectors.toList());
    }
}
