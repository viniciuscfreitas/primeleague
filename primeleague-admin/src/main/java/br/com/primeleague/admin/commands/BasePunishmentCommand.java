package br.com.primeleague.admin.commands;

import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.api.events.PlayerPunishedEvent;
import br.com.primeleague.api.enums.PunishmentSeverity;
import br.com.primeleague.admin.PrimeLeagueAdmin;
import br.com.primeleague.admin.managers.AdminManager;
import br.com.primeleague.admin.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.Calendar;
import java.util.Date;
import java.util.UUID;

/**
 * Classe base para comandos de punição.
 * REFATORADO: Remove toda lógica de persistência, delegando para AdminManager.
 * Agora é apenas um controlador que coordena operações via AdminManager.
 * 
 * @author PrimeLeague Team
 * @version 2.0 (Refatorado para arquitetura consolidada)
 */
public abstract class BasePunishmentCommand {
    
    protected final PrimeLeagueAdmin plugin;
    protected final AdminManager adminManager;
    
    /**
     * Construtor que recebe a instância do plugin principal.
     * REFATORADO: Inicializa AdminManager para operações de persistência.
     * 
     * @param plugin Instância do plugin PrimeLeagueAdmin
     */
    public BasePunishmentCommand(PrimeLeagueAdmin plugin) {
        this.plugin = plugin;
        this.adminManager = AdminManager.getInstance();
    }
    
    /**
     * Registra uma punição usando AdminManager.
     * REFATORADO: Remove queries SQL diretas, usa AdminManager.
     * 
     * @param targetUuid UUID do jogador punido
     * @param targetName Nome do jogador punido
     * @param authorUuid UUID do autor da punição
     * @param authorName Nome do autor da punição
     * @param type Tipo de punição
     * @param reason Motivo da punição
     * @param expiresAt Data de expiração (null para permanente)
     * @return true se registrado com sucesso, false caso contrário
     */
    protected boolean registerPunishment(UUID targetUuid, String targetName, UUID authorUuid, String authorName, 
                                       Punishment.Type type, String reason, Date expiresAt) {
        try {
            // Criar objeto Punishment
            Punishment punishment = new Punishment();
            punishment.setTargetUuid(targetUuid);
            punishment.setTargetName(targetName);
            punishment.setAuthorUuid(authorUuid);
            punishment.setAuthorName(authorName);
            punishment.setType(type);
            punishment.setReason(reason);
            punishment.setCreatedAt(new java.sql.Timestamp(System.currentTimeMillis()));
            punishment.setExpiresAt(expiresAt != null ? new java.sql.Timestamp(expiresAt.getTime()) : null);
            punishment.setActive(true);
            
            // Aplicar punição via AdminManager
            boolean success = adminManager.applyPunishment(punishment);
            
            if (success) {
                // Disparar evento de punição
                dispatchPlayerPunishedEvent(targetUuid, targetName, authorName, 
                                          getSeverityForType(type), reason, expiresAt);
            }
            
            return success;
            
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao registrar punição: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove uma punição ativa usando AdminManager.
     * REFATORADO: Remove queries SQL diretas, usa AdminManager.
     * 
     * @param targetUuid UUID do jogador
     * @param type Tipo de punição a ser removida
     * @param pardonerUuid UUID de quem removeu a punição
     * @param pardonerName Nome de quem removeu a punição
     * @param pardonReason Motivo da remoção
     * @return true se removido com sucesso, false caso contrário
     */
    protected boolean removePunishment(UUID targetUuid, Punishment.Type type, UUID pardonerUuid, 
                                     String pardonerName, String pardonReason) {
        try {
            return adminManager.pardonPunishment(targetUuid, type, pardonerUuid, pardonReason);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao remover punição: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Verifica se um jogador tem uma punição ativa usando AdminManager.
     * REFATORADO: Remove queries SQL diretas, usa AdminManager.
     * 
     * @param targetUuid UUID do jogador
     * @param type Tipo de punição
     * @return true se tem punição ativa, false caso contrário
     */
    protected boolean hasActivePunishment(UUID targetUuid, Punishment.Type type) {
        try {
            Punishment activePunishment = adminManager.getActivePunishment(targetUuid, type);
            return activePunishment != null;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao verificar punição ativa: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Busca o UUID de um jogador pelo nome usando DataManager.
     * REFATORADO: Remove queries SQL diretas, usa DataManager.
     * 
     * @param playerName Nome do jogador
     * @return UUID do jogador, ou null se não encontrado
     */
    protected UUID findPlayerUUID(String playerName) {
        try {
            return PrimeLeagueAPI.getDataManager().getPlayerUUID(playerName);
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao buscar UUID: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Verifica se um perfil de jogador existe usando DataManager.
     * REFATORADO: Remove queries SQL diretas, usa DataManager.
     * 
     * @param playerUuid UUID do jogador
     * @return true se o perfil existe, false caso contrário
     */
    protected boolean profileExists(UUID playerUuid) {
        try {
            return PrimeLeagueAPI.getDataManager().loadOfflinePlayerProfile(playerUuid) != null;
        } catch (Exception e) {
            Bukkit.getLogger().severe("Erro ao verificar perfil: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Constrói o motivo a partir dos argumentos.
     * 
     * @param args Argumentos do comando
     * @param startIndex Índice inicial para o motivo
     * @return Motivo construído
     */
    protected String buildReason(String[] args, int startIndex) {
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
     * Converte uma string de tempo para Date.
     * Formatos aceitos: 1d (1 dia), 2h (2 horas), 30m (30 minutos), 1w (1 semana)
     * 
     * @param timeString String de tempo
     * @return Date com a data de expiração, ou null se inválido
     */
    protected Date parseTimeString(String timeString) {
        if (timeString == null || timeString.isEmpty()) {
            return null;
        }
        
        try {
            char unit = timeString.charAt(timeString.length() - 1);
            int amount = Integer.parseInt(timeString.substring(0, timeString.length() - 1));
            
            Calendar cal = Calendar.getInstance();
            
            switch (unit) {
                case 's': // segundos
                    cal.add(Calendar.SECOND, amount);
                    break;
                case 'm': // minutos
                    cal.add(Calendar.MINUTE, amount);
                    break;
                case 'h': // horas
                    cal.add(Calendar.HOUR, amount);
                    break;
                case 'd': // dias
                    cal.add(Calendar.DAY_OF_MONTH, amount);
                    break;
                case 'w': // semanas
                    cal.add(Calendar.WEEK_OF_YEAR, amount);
                    break;
                case 'M': // meses
                    cal.add(Calendar.MONTH, amount);
                    break;
                case 'y': // anos
                    cal.add(Calendar.YEAR, amount);
                    break;
                default:
                    return null;
            }
            
            return cal.getTime();
            
        } catch (NumberFormatException e) {
            return null;
        }
    }
    
    /**
     * Envia mensagem para todos os staff online.
     * 
     * @param message Mensagem a ser enviada
     */
    protected void broadcastToStaff(String message) {
        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("primeleague.admin.staff")) {
                player.sendMessage(message);
            }
        }
    }
    
    /**
     * Formata uma data para exibição.
     * 
     * @param date Data a ser formatada
     * @return String formatada
     */
    protected String formatDate(Date date) {
        if (date == null) {
            return "Permanente";
        }
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("dd/MM/yyyy HH:mm");
        return sdf.format(date);
    }
    
    /**
     * Dispara o evento PlayerPunishedEvent.
     * 
     * @param targetUuid UUID do jogador punido
     * @param targetName Nome do jogador punido
     * @param authorName Nome do autor da punição
     * @param severity Severidade da punição
     * @param reason Motivo da punição
     * @param expiresAt Data de expiração
     */
    protected void dispatchPlayerPunishedEvent(UUID targetUuid, String targetName, String authorName, 
                                              PunishmentSeverity severity, String reason, Date expiresAt) {
        try {
            long duration = expiresAt != null ? expiresAt.getTime() - System.currentTimeMillis() : -1;
            
            PlayerPunishedEvent event = new PlayerPunishedEvent(
                targetUuid.toString(),
                targetName,
                "SYSTEM", // UUID do autor (será implementado quando tivermos sistema de autenticação)
                authorName,
                severity,
                reason,
                duration
            );
            
            Bukkit.getPluginManager().callEvent(event);
            
        } catch (Exception e) {
            Bukkit.getLogger().warning("Erro ao disparar PlayerPunishedEvent: " + e.getMessage());
        }
    }
    
    /**
     * Mapeia o tipo de punição para a severidade correspondente.
     * 
     * @param type Tipo de punição
     * @return Severidade correspondente
     */
    protected PunishmentSeverity getSeverityForType(Punishment.Type type) {
        switch (type) {
            case WARN:
                return PunishmentSeverity.LEVE;
            case KICK:
                return PunishmentSeverity.LEVE;
            case MUTE:
                return PunishmentSeverity.MEDIA;
            case BAN:
                return PunishmentSeverity.SERIA;
            default:
                return PunishmentSeverity.LEVE;
        }
    }
}
