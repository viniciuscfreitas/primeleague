package br.com.primeleague.clans.commands;

import br.com.primeleague.clans.PrimeLeagueClans;
import br.com.primeleague.clans.manager.ClanManager;
import br.com.primeleague.clans.manager.PromoteResult;
import br.com.primeleague.clans.manager.DemoteResult;
import br.com.primeleague.clans.manager.KickResult;
import br.com.primeleague.clans.manager.SetFounderResult;
import br.com.primeleague.clans.model.Clan;
import br.com.primeleague.clans.model.ClanPlayer;
import br.com.primeleague.api.dto.ClanLogDTO;
import br.com.primeleague.api.dto.ClanMemberInfo;
import br.com.primeleague.api.dto.ClanRankingInfoDTO;
import br.com.primeleague.api.enums.LogActionType;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import org.bukkit.Bukkit;

import java.util.List;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * Executor do comando principal /clan.
 * Gerencia todos os subcomandos do sistema de clãs.
 *
 * @version 1.0
 * @author PrimeLeague Team
 */
public class ClanCommand implements CommandExecutor {

    private final PrimeLeagueClans plugin;
    private final ClanManager clanManager;

    public ClanCommand(PrimeLeagueClans plugin) {
        this.plugin = plugin;
        this.clanManager = plugin.getClanManager();
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        Player player = (Player) sender;
        
        // BARRERA DE SEGURANÇA: Verificar se o perfil do jogador está carregado
        if (PrimeLeagueAPI.getDataManager().isLoading(player.getUniqueId())) {
            player.sendMessage("§cSeu perfil ainda está carregando. Tente novamente em um instante.");
            return true;
        }

        if (args.length == 0) {
            showHelp(player);
            return true;
        }

        String subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "create":
                handleCreate(player, args);
                break;
            case "invite":
                handleInvite(player, args);
                break;
            case "kick":
                handleKick(player, args);
                break;
            case "promote":
                handlePromote(player, args);
                break;
            case "demote":
                handleDemote(player, args);
                break;
            case "setfounder":
                handleSetFounder(player, args);
                break;
            case "leave":
                handleLeave(player);
                break;
            case "disband":
                handleDisband(player);
                break;
            case "accept":
                handleAccept(player);
                break;
            case "deny":
                handleDeny(player);
                break;
            case "info":
                handleInfo(player, args);
                break;
            case "members":
                handleMembers(player, args);
                break;
            case "logs":
                handleLogs(player, args);
                break;
            case "stats":
                handleStats(player, args);
                break;
            case "profile":
                handleProfile(player, args);
                break;
            case "friendlyfire":
                handleFriendlyFire(player, args);
                break;
            case "sanction":
                handleSanction(player, args);
                break;
            case "ally":
                handleAlly(player, args);
                break;
            case "rival":
                handleRival(player, args);
                break;
            case "list":
            case "rank":
                handleList(player, args);
                break;
            case "help":
                showHelp(player);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Subcomando desconhecido: " + subCommand);
                showHelp(player);
                break;
        }

        return true;
    }

    /**
     * Mostra a ajuda do comando.
     */
    private void showHelp(Player player) {
        player.sendMessage(ChatColor.GOLD + "=== Sistema de Clãs ===");
        player.sendMessage(ChatColor.YELLOW + "/clan create <tag> <nome> " + ChatColor.WHITE + "- Criar um clã");
        player.sendMessage(ChatColor.YELLOW + "/clan invite <jogador> " + ChatColor.WHITE + "- Convidar jogador");
        player.sendMessage(ChatColor.YELLOW + "/clan accept " + ChatColor.WHITE + "- Aceitar convite");
        player.sendMessage(ChatColor.YELLOW + "/clan deny " + ChatColor.WHITE + "- Recusar convite");
        player.sendMessage(ChatColor.YELLOW + "/clan kick <jogador> " + ChatColor.WHITE + "- Expulsar membro");
        player.sendMessage(ChatColor.YELLOW + "/clan promote <jogador> " + ChatColor.WHITE + "- Promover membro");
        player.sendMessage(ChatColor.YELLOW + "/clan demote <jogador> " + ChatColor.WHITE + "- Rebaixar membro");
        player.sendMessage(ChatColor.YELLOW + "/clan setfounder <jogador> " + ChatColor.WHITE + "- Transferir fundador");
        player.sendMessage(ChatColor.YELLOW + "/clan leave " + ChatColor.WHITE + "- Sair do clã");
        player.sendMessage(ChatColor.YELLOW + "/clan disband " + ChatColor.WHITE + "- Dissolver clã");
        player.sendMessage(ChatColor.YELLOW + "/clan info [tag] " + ChatColor.WHITE + "- Informações do clã");
        player.sendMessage(ChatColor.YELLOW + "/clan list [-pontos|-membros|-kdr] [página] " + ChatColor.WHITE + "- Ranking de clãs");
        player.sendMessage(ChatColor.YELLOW + "/clan logs [página] " + ChatColor.WHITE + "- Histórico do clã");
        player.sendMessage(ChatColor.YELLOW + "/clan stats [tag] " + ChatColor.WHITE + "- Estatísticas do clã");
        player.sendMessage(ChatColor.YELLOW + "/clan profile <jogador> " + ChatColor.WHITE + "- Perfil do jogador");
        player.sendMessage(ChatColor.YELLOW + "/clan friendlyfire <on|off> " + ChatColor.WHITE + "- Controlar friendly fire");
        player.sendMessage(ChatColor.YELLOW + "/clan sanction <tag> <view|add|set|remove|pardon> [valor] " + ChatColor.WHITE + "- Gerenciar sanções");
        player.sendMessage(ChatColor.YELLOW + "/clan ally <tag> " + ChatColor.WHITE + "- Enviar pedido de aliança");
        player.sendMessage(ChatColor.YELLOW + "/clan ally accept <tag> " + ChatColor.WHITE + "- Aceitar aliança");
        player.sendMessage(ChatColor.YELLOW + "/clan ally end <tag> " + ChatColor.WHITE + "- Terminar aliança");
        player.sendMessage(ChatColor.YELLOW + "/clan rival <tag> " + ChatColor.WHITE + "- Declarar rivalidade");
        player.sendMessage(ChatColor.YELLOW + "/c <mensagem> " + ChatColor.WHITE + "- Chat do clã");
    }

    /**
     * Manipula o comando /clan create.
     */
    private void handleCreate(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.create")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para criar clãs!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso: /clan create <tag> <nome>");
            return;
        }

        String tag = args[1];
        
        // Construir o nome completo a partir do terceiro argumento em diante
        StringBuilder nameBuilder = new StringBuilder();
        for (int i = 2; i < args.length; i++) {
            if (i > 2) nameBuilder.append(" ");
            nameBuilder.append(args[i]);
        }
        String name = nameBuilder.toString();

        // Verificar se o jogador já pertence a um clã
        Clan currentClan = clanManager.getClanByPlayer(player);
        if (currentClan != null) {
            player.sendMessage(ChatColor.RED + "Você já pertence ao clã " + currentClan.getTag() + "!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.createClanAsync(tag, name, player, (clan) -> {
            // HARDENING: Verificar se o jogador ainda está online
            if (!player.isOnline()) return; // Jogador desconectou
            
            if (clan != null) {
                player.sendMessage(ChatColor.GREEN + "Clã " + clan.getTag() + " (" + clan.getName() + ") criado com sucesso!");
                player.sendMessage(ChatColor.YELLOW + "Use /clan invite <jogador> para convidar membros.");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao criar clã. Verifique se a tag e nome estão disponíveis.");
            }
        });
    }

    /**
     * Manipula o comando /clan invite.
     * REFATORADO: Usa o novo método assíncrono sendInvitationAsync com hardening.
     */
    private void handleInvite(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.invite")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para convidar jogadores!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan invite <jogador>");
            return;
        }

        String targetName = args[1];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            player.sendMessage(ChatColor.RED + "Jogador " + targetName + " não encontrado!");
            return;
        }

        // Verificar se o jogador pode convidar
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.canInvite()) {
            player.sendMessage(ChatColor.RED + "Você não pode convidar jogadores para o clã!");
            return;
        }

        // Verificar se o alvo já pertence a um clã
        Clan targetClan = clanManager.getClanByPlayer(target);
        if (targetClan != null) {
            player.sendMessage(ChatColor.RED + target.getName() + " já pertence ao clã " + targetClan.getTag() + "!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.sendInvitationAsync(player, target, (success) -> {
            // HARDENING: Verificar se os jogadores ainda estão online
            if (!player.isOnline()) return; // Jogador desconectou
            if (!target.isOnline()) return; // Alvo desconectou
            
            if (success) {
                // Mensagem de sucesso já enviada pelo ClanManager
                player.sendMessage(ChatColor.GREEN + "Convite enviado para " + target.getName() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao enviar convite. Verifique se o jogador está online e não pertence a outro clã.");
            }
        });
    }

    /**
     * Manipula o comando /clan kick.
     */
    private void handleKick(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.kick")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para expulsar membros!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan kick <jogador>");
            return;
        }

        String targetName = args[1];

        // Verificar se o jogador pertence a um clã
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        // Verificar se o alvo pertence ao mesmo clã
        ClanPlayer targetClanPlayer = clanManager.getClanPlayerByName(targetName);
        if (targetClanPlayer == null || !clan.equals(targetClanPlayer.getClan())) {
            player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
            return;
        }

        // VERIFICAÇÃO DE PERMISSÃO CORRETA AQUI
        if (!clanPlayer.canKick(targetClanPlayer)) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para expulsar este membro!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.kickPlayerFromClanAsync(clan, targetName, player.getName(), (result) -> {
            // HARDENING: Verificar se os jogadores ainda estão online
            if (!player.isOnline()) return; // Jogador desconectou
            
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + targetName + " foi expulso do clã!");
                    
                    // Notificar o clã sobre a expulsão
                    clanManager.notifyClanMembers(clan, ChatColor.RED + targetName + " foi expulso do clã por " + player.getName() + ".");
                    
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.RED + "Você foi expulso do clã " + clan.getTag() + "!");
                    }
                    break;
                case PLAYER_NOT_FOUND:
                    player.sendMessage(ChatColor.RED + targetName + " não foi encontrado ou não pertence ao seu clã!");
                    break;
                case NOT_IN_SAME_CLAN:
                    player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
                    break;
                case CANNOT_KICK_SELF:
                    player.sendMessage(ChatColor.RED + "Você não pode expulsar a si mesmo! Use /clan leave para sair.");
                    break;
                case CANNOT_KICK_LEADER:
                    player.sendMessage(ChatColor.RED + "Você não pode expulsar o líder do clã!");
                    break;
            }
        });
    }

    /**
     * Manipula o comando /clan promote.
     */
    private void handlePromote(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.promote")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para promover membros!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan promote <jogador>");
            return;
        }

        String targetName = args[1];

        // Verificar se o jogador pode promover
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.canPromote()) {
            player.sendMessage(ChatColor.RED + "Você não pode promover membros do clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        // Verificar se o alvo pertence ao mesmo clã
        ClanPlayer targetClanPlayer = clanManager.getClanPlayerByName(targetName);
        if (targetClanPlayer == null || !clan.equals(targetClanPlayer.getClan())) {
            player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
            return;
        }

        // Verificar se não está tentando promover a si mesmo
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "Você não pode promover a si mesmo!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.promotePlayerAsync(clan, targetName, (result) -> {
            // HARDENING: Verificar se os jogadores ainda estão online
            if (!player.isOnline()) return; // Jogador desconectou
            
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + targetName + " foi promovido a oficial!");
                    
                    // Notificar o clã sobre a promoção
                    clanManager.notifyClanMembers(clan, ChatColor.AQUA + targetName + " foi promovido a Oficial por " + player.getName() + ".");
                    
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.GREEN + "Você foi promovido a oficial no clã " + clan.getTag() + "!");
                    }
                    break;
                case PLAYER_NOT_FOUND:
                    player.sendMessage(ChatColor.RED + targetName + " não foi encontrado ou não pertence ao seu clã!");
                    break;
                case NOT_IN_SAME_CLAN:
                    player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
                    break;
                case ALREADY_LEADER:
                    player.sendMessage(ChatColor.RED + targetName + " já é o líder do clã!");
                    break;
                case ALREADY_OFFICER:
                    player.sendMessage(ChatColor.RED + targetName + " já é um oficial do clã!");
                    break;
            }
        });
    }

    /**
     * Manipula o comando /clan demote.
     */
    private void handleDemote(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.demote")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para rebaixar membros!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan demote <jogador>");
            return;
        }

        String targetName = args[1];

        // Verificar se o jogador pode rebaixar
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.canDemote()) {
            player.sendMessage(ChatColor.RED + "Você não pode rebaixar membros do clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        // Verificar se o alvo pertence ao mesmo clã
        ClanPlayer targetClanPlayer = clanManager.getClanPlayerByName(targetName);
        if (targetClanPlayer == null || !clan.equals(targetClanPlayer.getClan())) {
            player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
            return;
        }

        // Verificar se não está tentando rebaixar a si mesmo
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "Você não pode rebaixar a si mesmo!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.demotePlayerAsync(clan, targetName, (result) -> {
            // HARDENING: Verificar se os jogadores ainda estão online
            if (!player.isOnline()) return; // Jogador desconectou
            
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + targetName + " foi rebaixado a membro!");
                    
                    // Notificar o clã sobre o rebaixamento
                    clanManager.notifyClanMembers(clan, ChatColor.GRAY + targetName + " foi rebaixado a Membro por " + player.getName() + ".");
                    
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.YELLOW + "Você foi rebaixado a membro no clã " + clan.getTag() + "!");
                    }
                    break;
                case PLAYER_NOT_FOUND:
                    player.sendMessage(ChatColor.RED + targetName + " não foi encontrado ou não pertence ao seu clã!");
                    break;
                case NOT_IN_SAME_CLAN:
                    player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
                    break;
                case CANNOT_DEMOTE_LEADER:
                    player.sendMessage(ChatColor.RED + "Você não pode rebaixar o líder do clã!");
                    break;
                case NOT_AN_OFFICER:
                    player.sendMessage(ChatColor.RED + targetName + " já é um membro, não pode ser rebaixado!");
                    break;
            }
        });
    }

    /**
     * Manipula o comando /clan setfounder.
     */
    private void handleSetFounder(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.setfounder")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para transferir o fundador!");
            return;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan setfounder <jogador>");
            return;
        }

        String targetName = args[1];

        // Verificar se o jogador é o fundador atual
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        // Verificar se é o fundador atual
        if (!clanPlayer.isFounder()) {
            player.sendMessage(ChatColor.RED + "Apenas o fundador pode transferir o cargo!");
            return;
        }

        // Verificar se o alvo pertence ao mesmo clã
        ClanPlayer targetClanPlayer = clanManager.getClanPlayerByName(targetName);
        if (targetClanPlayer == null || !clan.equals(targetClanPlayer.getClan())) {
            player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
            return;
        }

        // Verificar se não está tentando transferir para si mesmo
        if (player.getName().equalsIgnoreCase(targetName)) {
            player.sendMessage(ChatColor.RED + "Você já é o fundador do clã!");
            return;
        }

        // Verificar se o alvo é um líder (apenas líderes podem se tornar fundadores)
        if (!targetClanPlayer.isLeader()) {
            player.sendMessage(ChatColor.RED + targetName + " deve ser um líder para se tornar fundador!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.setFounderAsync(clan, targetName, (result) -> {
            // HARDENING: Verificar se os jogadores ainda estão online
            if (!player.isOnline()) return; // Jogador desconectou
            
            switch (result) {
                case SUCCESS:
                    player.sendMessage(ChatColor.GREEN + targetName + " é agora o novo fundador do clã!");
                    
                    // Notificar o clã sobre a transferência
                    clanManager.notifyClanMembers(clan, ChatColor.GOLD + "O trono foi passado! " + targetName + " é agora o novo fundador do clã!");
                    
                    Player targetPlayer = Bukkit.getPlayer(targetName);
                    if (targetPlayer != null && targetPlayer.isOnline()) {
                        targetPlayer.sendMessage(ChatColor.GOLD + "Você é agora o novo fundador do clã " + clan.getTag() + "!");
                    }
                    break;
                case PLAYER_NOT_FOUND:
                    player.sendMessage(ChatColor.RED + targetName + " não foi encontrado ou não pertence ao seu clã!");
                    break;
                case NOT_IN_SAME_CLAN:
                    player.sendMessage(ChatColor.RED + targetName + " não pertence ao seu clã!");
                    break;
                case NOT_LEADER:
                    player.sendMessage(ChatColor.RED + targetName + " deve ser um líder para se tornar fundador!");
                    break;
                case ALREADY_FOUNDER:
                    player.sendMessage(ChatColor.RED + targetName + " já é o fundador do clã!");
                    break;
            }
        });
    }

    /**
     * Manipula o comando /clan leave.
     */
    private void handleLeave(Player player) {
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        
        // Verificar se é o fundador
        if (clanPlayer.isFounder()) {
            player.sendMessage(ChatColor.RED + "O fundador não pode sair do clã! Use /clan disband para dissolver o clã ou /clan setfounder para transferir o cargo.");
            // Log de segurança para depuração futura
            plugin.getLogger().warning("O jogador " + player.getName() + " (Fundador do clã " + clan.getTag() + ") tentou usar /clan leave e foi bloqueado.");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.removeMemberAsync(clan, player.getName(), (success) -> {
            // HARDENING: Verificar se o jogador ainda está online
            if (!player.isOnline()) return; // Jogador desconectou
            
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Você saiu do clã " + clan.getTag() + "!");
                // Notificar o clã sobre a saída
                clanManager.notifyClanMembers(clan, ChatColor.YELLOW + player.getName() + " saiu do clã.");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao sair do clã!");
            }
        });
    }

    /**
     * Manipula o comando /clan disband.
     */
    private void handleDisband(Player player) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.disband")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para dissolver clãs!");
            return;
        }

        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        
        // Verificar se é o fundador
        if (!clanPlayer.isFounder()) {
            player.sendMessage(ChatColor.RED + "Apenas o fundador pode dissolver o clã!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.disbandClanAsync(clan, (success) -> {
            // HARDENING: Verificar se o jogador ainda está online
            if (!player.isOnline()) return; // Jogador desconectou
            
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Clã " + clan.getTag() + " foi dissolvido!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao dissolver o clã!");
            }
        });
    }

    /**
     * Manipula o comando /clan info.
     */
    private void handleInfo(Player player, String[] args) {
        Clan clan;
        
        if (args.length > 1) {
            // Buscar clã específico
            String tag = args[1];
            clan = clanManager.getClanByTag(tag);
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Clã " + tag + " não encontrado!");
                return;
            }
        } else {
            // Mostrar clã do jogador
            clan = clanManager.getClanByPlayer(player);
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
                return;
            }
        }

        // Obter informações detalhadas dos membros
        List<ClanMemberInfo> members = clanManager.getClanMembers(clan);
        
        // Calcular estatísticas
        int totalMembers = members.size();
        int onlineMembers = 0;
        int officerCount = 0;
        String founderName = clan.getFounderName();
        
        for (ClanMemberInfo member : members) {
            if (member.isOnline()) {
                onlineMembers++;
            }
            if (member.getRole() == br.com.primeleague.api.enums.ClanRole.LIDER) {
                officerCount++;
            }
        }
        
        int regularMembers = totalMembers - officerCount - 1; // -1 para o fundador

        player.sendMessage(ChatColor.GOLD + "=== " + clan.getTag() + " [" + clan.getName() + "] ===");
        player.sendMessage(ChatColor.YELLOW + "Fundador: " + ChatColor.WHITE + founderName);
        player.sendMessage(ChatColor.YELLOW + "Membros: " + ChatColor.WHITE + totalMembers + " (" + ChatColor.GREEN + onlineMembers + " Online" + ChatColor.WHITE + ")");
        player.sendMessage(ChatColor.YELLOW + "  - Líderes: " + ChatColor.WHITE + officerCount);
        player.sendMessage(ChatColor.YELLOW + "  - Membros: " + ChatColor.WHITE + regularMembers);
        player.sendMessage(ChatColor.GRAY + "Use /clan members para ver a lista detalhada de membros.");
    }

    /**
     * Manipula o comando /clan accept.
     * REFATORADO: Usa o novo método assíncrono acceptInvitationAsync com hardening.
     */
    private void handleAccept(Player player) {
        // Verificar se o jogador já pertence a um clã
        Clan currentClan = clanManager.getClanByPlayer(player);
        if (currentClan != null) {
            player.sendMessage(ChatColor.RED + "Você já pertence ao clã " + currentClan.getTag() + "!");
            return;
        }

        // REFATORADO: Usar método assíncrono com callback
        clanManager.acceptInvitationAsync(player, (success) -> {
            // HARDENING: Verificar se o jogador ainda está online
            if (!player.isOnline()) return; // Jogador desconectou
            
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Convite aceito! Bem-vindo ao clã!");
            } else {
                player.sendMessage(ChatColor.RED + "Você não tem convites pendentes ou o convite expirou!");
            }
        });
    }

    /**
     * Manipula o comando /clan deny.
     * REFATORADO: Usa o novo método assíncrono denyInvitationAsync com hardening.
     */
    private void handleDeny(Player player) {
        // REFATORADO: Usar método assíncrono com callback
        clanManager.denyInvitationAsync(player, (success) -> {
            // HARDENING: Verificar se o jogador ainda está online
            if (!player.isOnline()) return; // Jogador desconectou
            
            if (success) {
                player.sendMessage(ChatColor.YELLOW + "Convite recusado.");
            } else {
                player.sendMessage(ChatColor.RED + "Você não tem convites pendentes ou o convite expirou!");
            }
        });
    }

    /**
     * Manipula o comando /clan stats.
     */
    private void handleStats(Player player, String[] args) {
        Clan clan;
        
        if (args.length > 1) {
            // Buscar clã específico
            String tag = args[1];
            clan = clanManager.getClanByTag(tag);
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Clã " + tag + " não encontrado!");
                return;
            }
        } else {
            // Mostrar clã do jogador
            clan = clanManager.getClanByPlayer(player);
            if (clan == null) {
                player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
                return;
            }
        }

        // Calcular estatísticas do clã
        int totalKills = 0;
        int totalDeaths = 0;
        java.util.List<ClanPlayer> topPlayers = new java.util.ArrayList<>();
        
        for (String memberName : clan.getAllMemberNames()) {
            ClanPlayer member = clanManager.getClanPlayerByName(memberName);
            if (member != null) {
                totalKills += member.getKills();
                totalDeaths += member.getDeaths();
                topPlayers.add(member);
            }
        }

        // Ordenar por KDR (decrescente)
        java.util.Collections.sort(topPlayers, new java.util.Comparator<ClanPlayer>() {
            @Override
            public int compare(ClanPlayer p1, ClanPlayer p2) {
                return Double.compare(p2.getKDR(), p1.getKDR());
            }
        });

        // Calcular KDR total do clã
        double clanKDR = totalDeaths == 0 ? (totalKills > 0 ? totalKills : 0.0) : (double) totalKills / totalDeaths;

        // Mostrar estatísticas
        player.sendMessage(ChatColor.GOLD + "=== Estatísticas do Clã " + clan.getTag() + " ===");
        player.sendMessage(ChatColor.YELLOW + "KDR Total: " + ChatColor.WHITE + String.format("%.2f", clanKDR));
        player.sendMessage(ChatColor.YELLOW + "Total de Kills: " + ChatColor.WHITE + totalKills);
        player.sendMessage(ChatColor.YELLOW + "Total de Deaths: " + ChatColor.WHITE + totalDeaths);
        player.sendMessage(ChatColor.YELLOW + "Melhores Jogadores:");
        
        // Mostrar top 5 jogadores
        int count = 0;
        for (ClanPlayer member : topPlayers) {
            if (count >= 5) break;
            player.sendMessage(ChatColor.GRAY + "  " + (count + 1) + ". " + ChatColor.WHITE + member.getPlayerName() + 
                              ChatColor.GRAY + " - KDR: " + ChatColor.WHITE + String.format("%.2f", member.getKDR()) +
                              ChatColor.GRAY + " (" + member.getKills() + "k/" + member.getDeaths() + "d)");
            count++;
        }
    }

    /**
     * Manipula o comando /clan profile.
     */
    private void handleProfile(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan profile <jogador>");
            return;
        }

        String targetName = args[1];
        ClanPlayer targetPlayer = clanManager.getClanPlayerByName(targetName);
        
        if (targetPlayer == null) {
            player.sendMessage(ChatColor.RED + "Jogador " + targetName + " não encontrado!");
            return;
        }

        // Mostrar perfil do jogador
        player.sendMessage(ChatColor.GOLD + "=== Perfil de " + targetPlayer.getPlayerName() + " ===");
        
        if (targetPlayer.hasClan()) {
            player.sendMessage(ChatColor.YELLOW + "Clã: " + ChatColor.WHITE + targetPlayer.getClan().getTag() + 
                              " [" + targetPlayer.getClan().getName() + "]");
            player.sendMessage(ChatColor.YELLOW + "Cargo: " + ChatColor.WHITE + targetPlayer.getRole().getDisplayName());
        } else {
            player.sendMessage(ChatColor.YELLOW + "Clã: " + ChatColor.GRAY + "Nenhum");
        }
        
        player.sendMessage(ChatColor.YELLOW + "KDR: " + ChatColor.WHITE + String.format("%.2f", targetPlayer.getKDR()));
        player.sendMessage(ChatColor.YELLOW + "Kills: " + ChatColor.WHITE + targetPlayer.getKills());
        player.sendMessage(ChatColor.YELLOW + "Deaths: " + ChatColor.WHITE + targetPlayer.getDeaths());
        
        if (targetPlayer.hasClan()) {
            long joinDate = targetPlayer.getJoinDate();
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
            String joinDateStr = sdf.format(new java.util.Date(joinDate));
            player.sendMessage(ChatColor.YELLOW + "Membro desde: " + ChatColor.WHITE + joinDateStr);
        }
    }

    /**
     * Manipula o comando /clan friendlyfire.
     */
    private void handleFriendlyFire(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan friendlyfire <on|off>");
            return;
        }

        // Verificar se o jogador pertence a um clã
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        
        // Verificar se é o líder
        if (!clanPlayer.isLeader()) {
            player.sendMessage(ChatColor.RED + "Apenas o líder pode alterar as configurações de friendly fire!");
            return;
        }

        String option = args[1].toLowerCase();
        boolean enableFriendlyFire;

        if (option.equals("on")) {
            enableFriendlyFire = true;
        } else if (option.equals("off")) {
            enableFriendlyFire = false;
        } else {
            player.sendMessage(ChatColor.RED + "Opção inválida! Use 'on' ou 'off'.");
            return;
        }

        try {
            // Atualizar configuração
            clan.setFriendlyFireEnabled(enableFriendlyFire);
            
            // Persistir no banco de dados
            // TODO: Implementar conversão para DTO
            // clanManager.getPlugin().getClanManager().getClanDAO().updateClanSettings(clan);

            // Notificar o clã
            String status = enableFriendlyFire ? "habilitado" : "desabilitado";
            player.sendMessage(ChatColor.GREEN + "Friendly fire " + status + " no clã!");
            clanManager.notifyClanMembers(clan, ChatColor.YELLOW + "Friendly fire foi " + status + " por " + player.getName() + ".");

        } catch (Exception e) {
            player.sendMessage(ChatColor.RED + "Erro ao atualizar configuração de friendly fire!");
            plugin.getLogger().severe("Erro ao atualizar friendly fire: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Manipula os comandos relacionados a alianças.
     */
    private void handleAlly(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan ally <tag> ou /clan ally accept <tag> ou /clan ally end <tag>");
            return;
        }

        // Verificar se o jogador pertence a um clã
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan playerClan = clanPlayer.getClan();
        
        // Verificar se é o líder
        if (!clanPlayer.isLeader()) {
            player.sendMessage(ChatColor.RED + "Apenas o líder pode gerenciar alianças!");
            return;
        }

        if (args.length >= 3 && args[1].equalsIgnoreCase("accept")) {
            // Aceitar aliança
            String targetTag = args[2];
            Clan targetClan = clanManager.getClanByTag(targetTag);
            
            if (targetClan == null) {
                player.sendMessage(ChatColor.RED + "Clã " + targetTag + " não encontrado!");
                return;
            }

            if (playerClan.equals(targetClan)) {
                player.sendMessage(ChatColor.RED + "Você não pode fazer aliança consigo mesmo!");
                return;
            }

            // TODO: [FUTURO] Implementar sistema de pedidos de aliança pendentes
            // Por enquanto, cria a aliança diretamente
            if (clanManager.createAlliance(playerClan, targetClan)) {
                player.sendMessage(ChatColor.GREEN + "Aliança criada com " + targetClan.getTag() + "!");
                clanManager.notifyClanMembers(playerClan, ChatColor.AQUA + "Aliança criada com " + targetClan.getTag() + "!");
                clanManager.notifyClanMembers(targetClan, ChatColor.AQUA + "Aliança criada com " + playerClan.getTag() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao criar aliança!");
            }

        } else if (args.length >= 3 && args[1].equalsIgnoreCase("end")) {
            // Terminar aliança
            String targetTag = args[2];
            Clan targetClan = clanManager.getClanByTag(targetTag);
            
            if (targetClan == null) {
                player.sendMessage(ChatColor.RED + "Clã " + targetTag + " não encontrado!");
                return;
            }

            if (clanManager.removeAlliance(playerClan, targetClan)) {
                player.sendMessage(ChatColor.YELLOW + "Aliança terminada com " + targetClan.getTag() + "!");
                clanManager.notifyClanMembers(playerClan, ChatColor.YELLOW + "Aliança terminada com " + targetClan.getTag() + "!");
                clanManager.notifyClanMembers(targetClan, ChatColor.YELLOW + "Aliança terminada com " + playerClan.getTag() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Não há aliança com " + targetClan.getTag() + "!");
            }

        } else {
            // Enviar pedido de aliança
            String targetTag = args[1];
            Clan targetClan = clanManager.getClanByTag(targetTag);
            
            if (targetClan == null) {
                player.sendMessage(ChatColor.RED + "Clã " + targetTag + " não encontrado!");
                return;
            }

            if (playerClan.equals(targetClan)) {
                player.sendMessage(ChatColor.RED + "Você não pode fazer aliança consigo mesmo!");
                return;
            }

            // TODO: [FUTURO] Implementar sistema de pedidos de aliança pendentes
            player.sendMessage(ChatColor.YELLOW + "Sistema de pedidos de aliança será implementado em breve!");
            player.sendMessage(ChatColor.YELLOW + "Use /clan ally accept " + targetTag + " para criar aliança diretamente.");
        }
    }

    /**
     * Manipula o comando /clan rival.
     */
    private void handleRival(Player player, String[] args) {
        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + "Uso: /clan rival <tag>");
            return;
        }

        // Verificar se o jogador pertence a um clã
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan playerClan = clanPlayer.getClan();
        
        // Verificar se é o líder
        if (!clanPlayer.isLeader()) {
            player.sendMessage(ChatColor.RED + "Apenas o líder pode declarar rivalidades!");
            return;
        }

        String targetTag = args[1];
        Clan targetClan = clanManager.getClanByTag(targetTag);
        
        if (targetClan == null) {
            player.sendMessage(ChatColor.RED + "Clã " + targetTag + " não encontrado!");
            return;
        }

        if (playerClan.equals(targetClan)) {
            player.sendMessage(ChatColor.RED + "Você não pode declarar rivalidade consigo mesmo!");
            return;
        }

        if (clanManager.declareRivalry(playerClan, targetClan)) {
            player.sendMessage(ChatColor.RED + "Rivalidade declarada com " + targetClan.getTag() + "!");
            clanManager.notifyClanMembers(playerClan, ChatColor.RED + "Rivalidade declarada com " + targetClan.getTag() + "!");
            clanManager.notifyClanMembers(targetClan, ChatColor.RED + "Rivalidade declarada por " + playerClan.getTag() + "!");
        } else {
            player.sendMessage(ChatColor.RED + "Erro ao declarar rivalidade!");
        }
    }

    /**
     * Manipula o comando /clan logs.
     */
    private void handleLogs(Player player, String[] args) {
        // Verificar se o jogador pertence a um clã
        ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
        if (clanPlayer == null || !clanPlayer.hasClan()) {
            player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã!");
            return;
        }

        Clan clan = clanPlayer.getClan();
        
        // Verificar permissões (apenas líderes e fundadores podem ver logs)
        if (!clanPlayer.isLeader() && !clanPlayer.isFounder()) {
            player.sendMessage(ChatColor.RED + "Apenas líderes e fundadores podem ver o histórico do clã!");
            return;
        }

        // Determinar a página
        int page = 1;
        if (args.length >= 2) {
            try {
                page = Integer.parseInt(args[1]);
                if (page < 1) page = 1;
            } catch (NumberFormatException e) {
                player.sendMessage(ChatColor.RED + "Página inválida! Use um número.");
                return;
            }
        }

        // Buscar logs do clã
        List<ClanLogDTO> logs = clanManager.getClanLogs(clan.getId(), page, 10); // 10 logs por página
        
        if (logs.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nenhum registro encontrado para a página " + page + ".");
            return;
        }

        // Exibir cabeçalho
        player.sendMessage(ChatColor.GOLD + "=== Histórico do Clã " + clan.getTag() + " (Página " + page + ") ===");
        
        // Exibir logs
        for (ClanLogDTO log : logs) {
            String timestamp = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss")
                .format(new java.util.Date(log.getTimestamp()));
            
            String actionText = getActionText(log.getActionType());
            String details = log.getDetails() != null ? log.getDetails() : "";
            
            String message = ChatColor.GRAY + "[" + timestamp + "] " + 
                           ChatColor.WHITE + log.getActorName() + " " + 
                           ChatColor.YELLOW + actionText;
            
            if (log.getTargetName() != null) {
                message += ChatColor.WHITE + " " + log.getTargetName();
            }
            
            if (!details.isEmpty()) {
                message += ChatColor.GRAY + " (" + details + ")";
            }
            
            player.sendMessage(message);
        }
        
        player.sendMessage(ChatColor.GRAY + "Use /clan logs <página> para ver mais registros.");
    }

    /**
     * Converte LogActionType para texto legível.
     */
    private String getActionText(LogActionType actionType) {
        switch (actionType) {
            case CLAN_CREATE: return "criou o clã";
            case CLAN_DISBAND: return "dissolveu o clã";
            case PLAYER_INVITE: return "convidou";
            case PLAYER_JOIN: return "entrou no clã";
            case PLAYER_LEAVE: return "saiu do clã";
            case PLAYER_KICK: return "expulsou";
            case PLAYER_PROMOTE: return "promoveu";
            case PLAYER_DEMOTE: return "rebaixou";
            case FOUNDER_CHANGE: return "transferiu fundador para";
            case ALLY_INVITE: return "enviou convite de aliança para";
            case ALLY_ACCEPT: return "aceitou aliança com";
            case ALLY_REMOVE: return "removeu aliança com";
            case SANCTION_ADD: return "aplicou sanção";
            case SANCTION_SET: return "definiu pontos de penalidade";
            case SANCTION_REMOVE: return "removeu pontos de penalidade";
            case SANCTION_REVERSED: return "reverteu sanção";
            case SANCTION_PARDON: return "perdoou clã";
            case PENALTY_POINTS_ADDED: return "adicionou pontos de penalidade";
            default: return "realizou ação";
        }
    }

    /**
     * Manipula o comando /clan sanction.
     */
    private void handleSanction(Player player, String[] args) {
        // Verificar permissão administrativa
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.sanction")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para gerenciar sanções de clã!");
            return;
        }

        if (args.length < 3) {
            player.sendMessage(ChatColor.RED + "Uso: /clan sanction <tag> <view|add|set|remove|pardon> [valor]");
            return;
        }

        String clanTag = args[1].toUpperCase();
        String action = args[2].toLowerCase();
        
        // Buscar o clã
        Clan clan = clanManager.getClanByTag(clanTag);
        if (clan == null) {
            player.sendMessage(ChatColor.RED + "Clã '" + clanTag + "' não encontrado!");
            return;
        }

        switch (action) {
            case "view":
                handleSanctionView(player, clan);
                break;
            case "add":
                handleSanctionAdd(player, clan, args);
                break;
            case "set":
                handleSanctionSet(player, clan, args);
                break;
            case "remove":
                handleSanctionRemove(player, clan, args);
                break;
            case "pardon":
                handleSanctionPardon(player, clan);
                break;
            default:
                player.sendMessage(ChatColor.RED + "Ação inválida! Use: view, add, set, remove ou pardon");
                break;
        }
    }

    /**
     * Exibe informações de sanção de um clã.
     */
    private void handleSanctionView(Player player, Clan clan) {
        int penaltyPoints = clanManager.getClanPenaltyPoints(clan.getId());
        int tier = clanManager.getClanSanctionTier(clan.getId());
        
        player.sendMessage(ChatColor.GOLD + "=== Sanções do Clã " + clan.getTag() + " ===");
        player.sendMessage(ChatColor.WHITE + "Pontos de Penalidade: " + ChatColor.RED + penaltyPoints);
        player.sendMessage(ChatColor.WHITE + "Tier de Sanção: " + ChatColor.YELLOW + getTierText(tier));
        
        if (tier > 0) {
            player.sendMessage(ChatColor.RED + "⚠ Este clã está sob sanção!");
        } else {
            player.sendMessage(ChatColor.GREEN + "✓ Este clã não possui sanções ativas.");
        }
    }

    /**
     * Adiciona pontos de penalidade a um clã.
     */
    private void handleSanctionAdd(Player player, Clan clan, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Uso: /clan sanction <tag> add <pontos>");
            return;
        }

        try {
            int points = Integer.parseInt(args[3]);
            if (points <= 0) {
                player.sendMessage(ChatColor.RED + "A quantidade de pontos deve ser maior que zero!");
                return;
            }

            boolean success = clanManager.addPenaltyPointsDirectly(clan.getId(), points, player.getName());
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Adicionados " + points + " pontos de penalidade ao clã " + clan.getTag() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao adicionar pontos de penalidade!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantidade de pontos inválida!");
        }
    }

    /**
     * Define os pontos de penalidade de um clã.
     */
    private void handleSanctionSet(Player player, Clan clan, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Uso: /clan sanction <tag> set <pontos>");
            return;
        }

        try {
            int points = Integer.parseInt(args[3]);
            if (points < 0) {
                player.sendMessage(ChatColor.RED + "A quantidade de pontos não pode ser negativa!");
                return;
            }

            boolean success = clanManager.setPenaltyPoints(clan.getId(), points, player.getName());
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Pontos de penalidade do clã " + clan.getTag() + " definidos para " + points + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao definir pontos de penalidade!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantidade de pontos inválida!");
        }
    }

    /**
     * Remove pontos de penalidade de um clã (remoção parcial).
     */
    private void handleSanctionRemove(Player player, Clan clan, String[] args) {
        if (args.length < 4) {
            player.sendMessage(ChatColor.RED + "Uso: /clan sanction <tag> remove <pontos>");
            return;
        }

        try {
            int points = Integer.parseInt(args[3]);
            if (points <= 0) {
                player.sendMessage(ChatColor.RED + "A quantidade de pontos a remover deve ser maior que zero!");
                return;
            }

            boolean success = clanManager.removePenaltyPoints(clan.getId(), points, player.getName());
            if (success) {
                player.sendMessage(ChatColor.GREEN + "Removidos " + points + " pontos de penalidade do clã " + clan.getTag() + "!");
            } else {
                player.sendMessage(ChatColor.RED + "Erro ao remover pontos de penalidade!");
            }
        } catch (NumberFormatException e) {
            player.sendMessage(ChatColor.RED + "Quantidade de pontos inválida!");
        }
    }

    /**
     * Remove todos os pontos de penalidade de um clã (perdão).
     */
    private void handleSanctionPardon(Player player, Clan clan) {
        boolean success = clanManager.pardonClan(clan.getId(), player.getName());
        if (success) {
            player.sendMessage(ChatColor.GREEN + "Clã " + clan.getTag() + " foi perdoado! Todos os pontos de penalidade foram removidos.");
        } else {
            player.sendMessage(ChatColor.RED + "Erro ao perdoar clã!");
        }
    }

    /**
     * Converte tier de sanção para texto legível.
     */
    private String getTierText(int tier) {
        switch (tier) {
            case 0: return "Nenhum";
            case 1: return "Tier 1 (Aviso)";
            case 2: return "Tier 2 (Multa)";
            case 3: return "Tier 3 (Suspensão)";
            case 4: return "Tier 4 (Desqualificação)";
            default: return "Desconhecido";
        }
    }
    
    /**
     * Manipula o comando /clan members [tag] [página]
     * Exibe uma lista detalhada e paginada dos membros do clã.
     * CORRIGIDO: Permite visualizar membros de outros clãs.
     */
    private void handleMembers(Player player, String[] args) {
        // Verificar permissão
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.members") && !PrimeLeagueAPI.hasPermission(player, "primeleague.clans.use")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return;
        }
        
        Clan clanToView;
        int pageArgIndex = 1; // Onde o número da página estaria se não houver tag

        // Lógica para determinar qual clã visualizar
        if (args.length > 1 && !args[1].matches("\\d+")) {
            // Um argumento de tag de clã foi fornecido
            String targetTag = args[1];
            clanToView = clanManager.getClanByTag(targetTag);
            pageArgIndex = 2; // O número da página é o próximo argumento
            if (clanToView == null) {
                player.sendMessage(ChatColor.RED + "Clã com a tag '" + targetTag + "' não encontrado.");
                return;
            }
        } else {
            // Nenhum argumento de tag, usar o clã do jogador
            ClanPlayer clanPlayer = clanManager.getClanPlayer(player);
            if (clanPlayer == null || clanPlayer.getClan() == null) {
                player.sendMessage(ChatColor.RED + "Você não pertence a nenhum clã. Use /clan members <tag>.");
                return;
            }
            clanToView = clanPlayer.getClan();
        }

        // Obter lista de membros
        List<ClanMemberInfo> members = clanManager.getClanMembers(clanToView);

        if (members.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "O clã " + clanToView.getTag() + " não possui membros.");
            return;
        }

        // Configuração de paginação
        int membersPerPage = 10;
        int totalPages = (int) Math.ceil((double) members.size() / membersPerPage);
        int currentPage = 1;

        if (args.length > pageArgIndex) {
            try {
                currentPage = Integer.parseInt(args[pageArgIndex]);
                if (currentPage < 1) currentPage = 1;
                if (currentPage > totalPages) currentPage = totalPages;
            } catch (NumberFormatException e) {
                // Ignora se não for um número
            }
        }

        int startIndex = (currentPage - 1) * membersPerPage;
        int endIndex = Math.min(startIndex + membersPerPage, members.size());

        player.sendMessage(ChatColor.GOLD + "=== Membros do Clã [" + clanToView.getTag() + "] (Página " + currentPage + "/" + totalPages + ") ===");
        for (int i = startIndex; i < endIndex; i++) {
            ClanMemberInfo member = members.get(i);
            displayMemberInfo(player, member);
        }

        if (totalPages > 1) {
            player.sendMessage(ChatColor.GRAY + "Use /clan members " + (args.length > 1 && !args[1].matches("\\d+") ? args[1] + " " : "") + "<página> para navegar.");
        }
    }
    
    /**
     * Exibe informações formatadas de um membro do clã.
     * CORRIGIDO: Usa a PrimeLeagueAPI como fallback para garantir o nome correto.
     */
    private void displayMemberInfo(Player player, ClanMemberInfo member) {
        String memberName = member.getPlayerName();

        // Fallback arquiteturalmente correto usando a API
        if (memberName == null || memberName.isEmpty()) {
            String fallbackName = br.com.primeleague.api.ProfileServiceRegistry.getPlayerName(java.util.UUID.fromString(member.getPlayerUuid()));
            if (fallbackName != null && !fallbackName.isEmpty()) {
                memberName = fallbackName;
            } else {
                memberName = "Nome não encontrado";
            }
        }

        // Determinar cor do cargo
        ChatColor roleColor;
        switch (member.getRole()) {
            case FUNDADOR:
                roleColor = ChatColor.RED;
                break;
            case LIDER:
                roleColor = ChatColor.GOLD;
                break;
            case MEMBRO:
                roleColor = ChatColor.WHITE;
                break;
            default:
                roleColor = ChatColor.GRAY;
        }
        
        // Determinar cor do status online/offline
        ChatColor statusColor = member.isOnline() ? ChatColor.GREEN : ChatColor.RED;
        String statusText = member.isOnline() ? "Online" : "Offline";
        
        // Formatar data de última vez visto (se offline)
        String lastSeenText = "";
        if (!member.isOnline() && member.getLastSeen() > 0) {
            long currentTime = System.currentTimeMillis();
            long timeDiff = currentTime - member.getLastSeen();
            
            if (timeDiff < 60000) { // Menos de 1 minuto
                lastSeenText = " (Visto agora)";
            } else if (timeDiff < 3600000) { // Menos de 1 hora
                long minutes = timeDiff / 60000;
                lastSeenText = " (Visto há " + minutes + " min)";
            } else if (timeDiff < 86400000) { // Menos de 1 dia
                long hours = timeDiff / 3600000;
                lastSeenText = " (Visto há " + hours + "h)";
            } else { // Mais de 1 dia
                long days = timeDiff / 86400000;
                lastSeenText = " (Visto há " + days + " dias)";
            }
        }
        
        // Construir mensagem
        String message = roleColor + "[" + member.getRoleName() + "] " + 
                        ChatColor.WHITE + memberName + 
                        ChatColor.GRAY + " - KDR: " + ChatColor.YELLOW + member.getKDR() + 
                        ChatColor.GRAY + " (" + member.getKills() + "/" + member.getDeaths() + ")" +
                        ChatColor.GRAY + " - " + statusColor + statusText + lastSeenText;
        
        player.sendMessage(message);
    }
    
    /**
     * Manipula o comando /clan list (ranking de clãs).
     */
    private void handleList(Player player, String[] args) {
        if (!PrimeLeagueAPI.hasPermission(player, "primeleague.clans.list")) {
            player.sendMessage(ChatColor.RED + "Você não tem permissão para ver o ranking de clãs!");
            return;
        }
        
        // Parsear argumentos
        String criteria = "ranking_points"; // Padrão
        int page = 1;
        
        for (String arg : args) {
            if (arg.startsWith("-")) {
                switch (arg.toLowerCase()) {
                    case "-pontos":
                        criteria = "ranking_points";
                        break;
                    case "-membros":
                        criteria = "member_count";
                        break;
                    case "-kdr":
                        criteria = "kdr";
                        break;
                    case "-kills":
                        criteria = "total_kills";
                        break;
                    case "-wins":
                        criteria = "total_wins";
                        break;
                }
            } else if (arg.matches("\\d+")) {
                page = Integer.parseInt(arg);
                if (page < 1) page = 1;
            }
        }
        
        // Configuração de paginação
        int pageSize = 5; // 5 clãs por página para melhor visualização
        int totalClans = clanManager.getTotalClans();
        int totalPages = (int) Math.ceil((double) totalClans / pageSize);
        
        if (page > totalPages && totalPages > 0) {
            page = totalPages;
        }
        
        // Buscar ranking
        List<ClanRankingInfoDTO> rankings = clanManager.getClanRankings(criteria, page, pageSize);
        
        if (rankings.isEmpty()) {
            player.sendMessage(ChatColor.YELLOW + "Nenhum clã encontrado no ranking.");
            return;
        }
        
        // Determinar título baseado no critério
        String title;
        switch (criteria) {
            case "ranking_points":
                title = "Pontos";
                break;
            case "member_count":
                title = "Membros";
                break;
            case "kdr":
                title = "KDR";
                break;
            case "total_kills":
                title = "Kills";
                break;
            case "total_wins":
                title = "Vitórias";
                break;
            default:
                title = "Pontos";
        }
        
        // Exibir cabeçalho
        player.sendMessage(ChatColor.GOLD + "=== Ranking de Clãs (" + title + ") - Página " + page + "/" + totalPages + " ===");
        
        // Exibir clãs do ranking
        for (ClanRankingInfoDTO ranking : rankings) {
            displayRankingInfo(player, ranking);
        }
        
        // Exibir informações de paginação
        if (totalPages > 1) {
            player.sendMessage(ChatColor.GRAY + "Use /clan list [-critério] <página> para navegar.");
            player.sendMessage(ChatColor.GRAY + "Critérios: -pontos, -membros, -kdr, -kills, -wins");
        }
    }
    
    /**
     * Exibe informações formatadas de um clã no ranking.
     */
    private void displayRankingInfo(Player player, ClanRankingInfoDTO ranking) {
        // Cor do rank
        ChatColor rankColor;
        if (ranking.getRank() == 1) {
            rankColor = ChatColor.GOLD; // Dourado para 1º lugar
        } else if (ranking.getRank() == 2) {
            rankColor = ChatColor.GRAY; // Prata para 2º lugar
        } else if (ranking.getRank() == 3) {
            rankColor = ChatColor.RED; // Bronze para 3º lugar
        } else {
            rankColor = ChatColor.WHITE; // Branco para os demais
        }
        
        // Linha principal do clã
        String mainLine = rankColor + String.valueOf(ranking.getRank()) + ". " +
                         ChatColor.AQUA + "[" + ranking.getTag() + "]" +
                         ChatColor.WHITE + " " + ranking.getName() + 
                         ChatColor.GRAY + " - Pts: " + ChatColor.YELLOW + String.valueOf(ranking.getRankingPoints());
        
        player.sendMessage(mainLine);
        
        // Linha de detalhes
        String detailsLine = ChatColor.GRAY + "   Fundador: " + ChatColor.WHITE + ranking.getFounderName() +
                           ChatColor.GRAY + " | Membros: " + ChatColor.WHITE + ranking.getMemberCount() +
                           ChatColor.GRAY + " | KDR: " + ChatColor.YELLOW + ranking.getClanKdr();
        
        player.sendMessage(detailsLine);
        
        // Linha de vitórias
        String winsLine = ChatColor.GRAY + "   Vitórias: " + ChatColor.GREEN + ranking.getWinsFormatted();
        player.sendMessage(winsLine);
        
        // Linha de status
        String statusLine = ChatColor.GRAY + "   Status: " + ranking.getStatusColor() + ranking.getStatusText() + ChatColor.WHITE;
        player.sendMessage(statusLine);
        
        // Separador
        player.sendMessage(ChatColor.GRAY + "-----------------------------------------------------");
    }
}
