package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.api.models.Punishment;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.core.models.PlayerProfile;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Date;
import java.util.UUID;

/**
 * Comando para banir jogadores do servidor.
 * 
 * REFATORADO para nova arquitetura:
 * - Usa AdminManager como fonte única da verdade
 * - Delega toda lógica de negócio para AdminManager
 * - Foca apenas em validação de sintaxe e permissões
 * - Remove acesso direto ao banco de dados
 * 
 * @author PrimeLeague Team
 * @version 2.0.0 (Refatorado para nova arquitetura)
 */
public class BanCommand implements CommandExecutor {
    
    private final PrimeLeagueAdmin plugin;
    private final AdminManager adminManager;
    
    /**
     * Construtor que recebe a instancia do plugin principal.
     * 
     * @param plugin Instancia do plugin PrimeLeagueAdmin
     */
    public BanCommand(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
        this.adminManager = plugin.getAdminManager();
    }
    
    @Override
    public boolean onCommand(final CommandSender sender, Command command, String label, String[] args) {
        // Verificar permissao
        if (!PrimeLeagueAPI.hasPermission((Player) sender, "primeleague.admin.ban")) {
            PrimeLeagueAPI.sendNoPermission((Player) sender);
            return true;
        }
        
        // BARRERA DE SEGURANÇA: Verificar se o perfil do jogador está carregado (apenas para jogadores)
        if (sender instanceof Player) {
            Player player = (Player) sender;
            if (PrimeLeagueAPI.getDataManager().isLoading(player.getUniqueId())) {
                player.sendMessage("§cSeu perfil ainda está carregando. Tente novamente em um instante.");
                return true;
            }
        }
        
        // Verificar argumentos
        if (args.length < 1) {
            PrimeLeagueAPI.sendUsage((Player) sender, "/ban <jogador> [motivo]");
            return true;
        }
        
        final String targetName = args[0];
        final String reason = args.length > 1 ? buildReason(args, 1) : "Sem motivo especificado";
        final UUID authorUuid = sender instanceof Player ? ((Player) sender).getUniqueId() : null;
        
        // Verificar se nao esta tentando banir a si mesmo
        if (sender instanceof Player && targetName.equalsIgnoreCase(sender.getName())) {
            PrimeLeagueAPI.sendError((Player) sender, "Voce nao pode banir a si mesmo.");
            return true;
        }
        
        // Executar ban de forma assincrona
        Bukkit.getScheduler().runTaskAsynchronously(this.plugin, new Runnable() {
            @Override
            public void run() {
                try {
                    // REFATORADO: Usar AdminManager para toda a lógica de negócio
                    final boolean success = executeBan(targetName, reason, authorUuid, sender);
                    
                    // Executar feedback na thread principal
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            if (success) {
                                // Notificar staff
                                PrimeLeagueAPI.sendSuccess((Player) sender, 
                                    "Jogador " + targetName + " foi banido permanentemente.");
                                
                                // Broadcast para staff
                                broadcastToStaff("§4[BAN] §f" + sender.getName() + " baniu " + targetName + 
                                               " permanentemente por: " + reason);
                            } else {
                                PrimeLeagueAPI.sendError((Player) sender, 
                                    "Erro ao executar ban. Verifique se o jogador existe.");
                            }
                        }
                    });
                    
                } catch (Exception e) {
                    Bukkit.getLogger().severe("Erro ao executar ban: " + e.getMessage());
                    e.printStackTrace();
                    
                    Bukkit.getScheduler().runTask(plugin, new Runnable() {
                        @Override
                        public void run() {
                            PrimeLeagueAPI.sendError((Player) sender, 
                                "Erro interno ao executar ban.");
                        }
                    });
                }
            }
        });
        
        return true;
    }
    
    /**
     * Executa o banimento de forma assíncrona.
     * REFATORADO: Usa player_id como identificador principal.
     * 
     * @param targetName Nome do jogador alvo
     * @param reason Motivo do banimento
     * @param authorUuid UUID do autor (pode ser null para Console)
     * @param sender CommandSender para feedback
     * @return true se o banimento foi aplicado com sucesso
     */
    private boolean executeBan(String targetName, String reason, UUID authorUuid, CommandSender sender) {
        try {
            // REFATORADO: Obter player_id do alvo via IdentityManager
            Integer targetPlayerId = getTargetPlayerId(targetName);
            if (targetPlayerId == null) {
                return false;
            }
            
            // REFATORADO: Obter player_id do autor via IdentityManager
            Integer authorPlayerId = null;
            if (authorUuid != null) {
                authorPlayerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByUuid(authorUuid);
                if (authorPlayerId == null) {
                    Bukkit.getLogger().warning("[BAN] Não foi possível obter player_id do autor: " + authorUuid);
                    return false;
                }
            }
            
            // REFATORADO: Verificar se já tem ban ativo via AdminManager usando player_id
            Punishment existingBan = adminManager.getActivePunishment(targetPlayerId, Punishment.Type.BAN);
            if (existingBan != null) {
                return false; // Já está banido
            }
            
            // REFATORADO: Criar objeto Punishment e usar AdminManager com player_id
            Punishment punishment = new Punishment();
            punishment.setType(Punishment.Type.BAN);
            punishment.setTargetPlayerId(targetPlayerId);
            punishment.setAuthorPlayerId(authorPlayerId);
            punishment.setReason(reason);
            punishment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            punishment.setExpiresAt(null); // Ban permanente
            punishment.setActive(true);
            
            // REFATORADO: Aplicar punição via AdminManager (inclui kick automático)
            adminManager.applyPunishment(punishment);
            
            // Assumir sucesso se chegou até aqui
            boolean success = true;
            if (success) {
                // Log do banimento com player_id
                String authorName = authorPlayerId != null ? 
                    PrimeLeagueAPI.getIdentityManager().getNameByPlayerId(authorPlayerId) : "Console";
                Bukkit.getLogger().info("[ADMIN] Ban aplicado para " + targetName + " (Player ID: " + targetPlayerId + ") por " + authorName);
            }
            
            return success;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao executar ban para " + targetName + ": " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Obtém o player_id do jogador alvo.
     * REFATORADO: Usa IdentityManager como fonte única da verdade.
     * 
     * @param targetName Nome do jogador
     * @return player_id do jogador ou null se não encontrado
     */
    private Integer getTargetPlayerId(String targetName) {
        // Primeiro, tentar jogador online
        Player targetPlayer = Bukkit.getPlayerExact(targetName);
        if (targetPlayer != null) {
            return PrimeLeagueAPI.getIdentityManager().getPlayerId(targetPlayer);
        }
        
        // Se não estiver online, usar IdentityManager para buscar por nome
        try {
            Integer playerId = PrimeLeagueAPI.getIdentityManager().getPlayerIdByName(targetName);
            if (playerId != null) {
                return playerId;
            }
        } catch (Exception e) {
            Bukkit.getLogger().warning("[BAN] Erro ao buscar player_id para " + targetName + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Constrói o motivo da punição a partir dos argumentos.
     * 
     * @param args Argumentos do comando
     * @param startIndex Índice inicial para o motivo
     * @return Motivo construído
     */
    private String buildReason(String[] args, int startIndex) {
        StringBuilder reason = new StringBuilder();
        for (int i = startIndex; i < args.length; i++) {
            if (i > startIndex) {
                reason.append(" ");
            }
            reason.append(args[i]);
        }
        return reason.toString();
    }
    
    /**
     * Envia broadcast para todos os staff online.
     * 
     * @param message Mensagem a ser enviada
     */
    private void broadcastToStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (PrimeLeagueAPI.hasPermission(player, "primeleague.admin.notify")) {
                player.sendMessage(message);
            }
        }
    }
}
