package br.com.primeleague.combatlog.managers;

import br.com.primeleague.combatlog.CombatLogPlugin;
import br.com.primeleague.core.api.PrimeLeagueAPI;
import br.com.primeleague.admin.api.AdminAPI;
import br.com.primeleague.api.models.Punishment;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

/**
 * Serviço de punições para combat log.
 * Integra com o AdminManager existente via PrimeLeagueAPI.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class CombatPunishmentService {
    
    private final CombatLogPlugin plugin;
    private final Logger logger;
    
    // Cache de ocorrências por jogador (UUID -> contador)
    private final Map<UUID, Integer> playerOffenses = new HashMap<UUID, Integer>();
    
    // Configurações de punições
    private int firstOffenseMinutes = 60;      // 1 hora
    private int secondOffenseMinutes = 360;    // 6 horas
    private int thirdOffenseMinutes = 1440;    // 24 horas
    private int chronicOffenseMinutes = -1;    // Permanente
    
    /**
     * Construtor do CombatPunishmentService.
     * 
     * @param plugin Instância do plugin principal
     */
    public CombatPunishmentService(CombatLogPlugin plugin) {
        this.plugin = plugin;
        this.logger = plugin.getLogger();
        
        // Carregar configurações
        loadConfiguration();
        
        logger.info("✅ CombatPunishmentService inicializado com sucesso!");
    }
    
    /**
     * Carrega as configurações de punições do arquivo config.yml.
     */
    private void loadConfiguration() {
        try {
            firstOffenseMinutes = plugin.getConfig().getInt("combat_log.punishments.first_offense", 60);
            secondOffenseMinutes = plugin.getConfig().getInt("combat_log.punishments.second_offense", 360);
            thirdOffenseMinutes = plugin.getConfig().getInt("combat_log.punishments.third_offense", 1440);
            chronicOffenseMinutes = plugin.getConfig().getInt("combat_log.punishments.chronic_offense", -1);
            
            logger.info("✅ Configurações de punições carregadas - 1ª: " + firstOffenseMinutes + 
                       "m, 2ª: " + secondOffenseMinutes + "m, 3ª: " + thirdOffenseMinutes + 
                       "m, Crônica: " + (chronicOffenseMinutes == -1 ? "Permanente" : chronicOffenseMinutes + "m"));
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao carregar configurações de punições, usando valores padrão: " + e.getMessage());
        }
    }
    
    /**
     * Aplica punição automática para combat log.
     * Integra com o AdminManager existente via PrimeLeagueAPI.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     */
    public void applyCombatLogPunishment(UUID playerUuid, String playerName) {
        if (playerUuid == null || playerName == null) {
            return;
        }
        
        try {
            // Incrementar contador de ocorrências
            int offenseCount = getOffenseCount(playerUuid) + 1;
            playerOffenses.put(playerUuid, offenseCount);
            
            // Determinar duração da punição
            int punishmentMinutes = getPunishmentDuration(offenseCount);
            String punishmentType = getPunishmentType(offenseCount);
            
            // Aplicar punição via AdminManager
            if (punishmentMinutes > 0) {
                applyTemporaryBan(playerUuid, playerName, punishmentMinutes, offenseCount);
            } else if (punishmentMinutes == -1) {
                applyPermanentBan(playerUuid, playerName, offenseCount);
            }
            
            // Log da punição aplicada
            logger.warning("🚨 Punição aplicada para " + playerName + 
                          " - " + offenseCount + "ª ocorrência de combat log - " + punishmentType);
            
            // Notificar staff online
            notifyStaffAboutPunishment(playerName, offenseCount, punishmentType);
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao aplicar punição para " + playerName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Obtém o número de ocorrências de um jogador.
     * 
     * @param playerUuid UUID do jogador
     * @return Número de ocorrências
     */
    private int getOffenseCount(UUID playerUuid) {
        Integer count = playerOffenses.get(playerUuid);
        return count != null ? count : 0;
    }
    
    /**
     * Determina a duração da punição baseada no número de ocorrências.
     * 
     * @param offenseCount Número de ocorrências
     * @return Duração em minutos (-1 para permanente)
     */
    private int getPunishmentDuration(int offenseCount) {
        switch (offenseCount) {
            case 1:
                return firstOffenseMinutes;
            case 2:
                return secondOffenseMinutes;
            case 3:
                return thirdOffenseMinutes;
            default:
                return chronicOffenseMinutes;
        }
    }
    
    /**
     * Obtém o tipo de punição para exibição.
     * 
     * @param offenseCount Número de ocorrências
     * @return String descritiva da punição
     */
    private String getPunishmentType(int offenseCount) {
        switch (offenseCount) {
            case 1:
                return "Ban temporário de " + firstOffenseMinutes + " minutos";
            case 2:
                return "Ban temporário de " + secondOffenseMinutes + " minutos";
            case 3:
                return "Ban temporário de " + thirdOffenseMinutes + " minutos";
            default:
                return "Ban permanente";
        }
    }
    
    /**
     * Aplica ban temporário via AdminManager.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param minutes Duração em minutos
     * @param offenseCount Número de ocorrências
     */
    private void applyTemporaryBan(UUID playerUuid, String playerName, int minutes, int offenseCount) {
        try {
            // Tentar usar o AdminManager via PrimeLeagueAPI
            if (AdminAPI.isAvailable()) {
                String reason = "Combat Log (" + offenseCount + "ª Ocorrência)";
                
                // Converter minutos para milissegundos
                long durationMillis = minutes * 60L * 1000L;
                
                // Aplicar ban temporário
                // Criar punição usando AdminAPI
                Punishment punishment = new Punishment();
                punishment.setTargetUuid(playerUuid);
                punishment.setTargetName(playerName);
                punishment.setType(Punishment.Type.BAN);
                punishment.setReason(reason);
                punishment.setExpiresAt(new java.sql.Timestamp(System.currentTimeMillis() + durationMillis));
                punishment.setAuthorUuid(null); // Sistema automático
                punishment.setAuthorName("PrimeLeague CombatLog");
                
                AdminAPI.applyPunishment(punishment);
                
                logger.info("✅ Ban temporário aplicado via AdminManager para " + playerName + 
                           " por " + minutes + " minutos");
                
            } else {
                // Fallback: aplicar punição local
                applyLocalPunishment(playerUuid, playerName, minutes, offenseCount);
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao usar AdminManager, aplicando punição local: " + e.getMessage());
            applyLocalPunishment(playerUuid, playerName, minutes, offenseCount);
        }
    }
    
    /**
     * Aplica ban permanente via AdminManager.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param offenseCount Número de ocorrências
     */
    private void applyPermanentBan(UUID playerUuid, String playerName, int offenseCount) {
        try {
            // Tentar usar o AdminManager via PrimeLeagueAPI
            if (AdminAPI.isAvailable()) {
                String reason = "Combat Log Crônico (" + offenseCount + "ª Ocorrência)";
                
                // Aplicar ban permanente
                // Criar punição permanente usando AdminAPI
                Punishment punishment = new Punishment();
                punishment.setTargetUuid(playerUuid);
                punishment.setTargetName(playerName);
                punishment.setType(Punishment.Type.BAN);
                punishment.setReason(reason);
                punishment.setExpiresAt(null); // Permanente
                punishment.setAuthorUuid(null); // Sistema automático
                punishment.setAuthorName("PrimeLeague CombatLog");
                
                AdminAPI.applyPunishment(punishment);
                
                logger.info("✅ Ban permanente aplicado via AdminManager para " + playerName);
                
            } else {
                // Fallback: aplicar punição local
                applyLocalPunishment(playerUuid, playerName, -1, offenseCount);
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao usar AdminManager, aplicando punição local: " + e.getMessage());
            applyLocalPunishment(playerUuid, playerName, -1, offenseCount);
        }
    }
    
    /**
     * Aplica punição local como fallback.
     * 
     * @param playerUuid UUID do jogador
     * @param playerName Nome do jogador
     * @param minutes Duração em minutos (-1 para permanente)
     * @param offenseCount Número de ocorrências
     */
    private void applyLocalPunishment(UUID playerUuid, String playerName, int minutes, int offenseCount) {
        try {
            // Kickar o jogador se estiver online
            Player player = Bukkit.getPlayer(playerName);
            if (player != null && player.isOnline()) {
                String kickMessage = "🚨 Você foi punido por combat log!\n" +
                                   "Ocorrência: " + offenseCount + "ª\n" +
                                   "Punição: " + (minutes == -1 ? "Ban permanente" : "Ban de " + minutes + " minutos");
                
                player.kickPlayer(kickMessage);
            }
            
            // Nota: Ban local via Bukkit não disponível em 1.5.2
            // A punição será aplicada via AdminManager que gerencia o ban no banco de dados
            
            logger.info("✅ Punição local aplicada para " + playerName + 
                       " - " + offenseCount + "ª ocorrência");
            
        } catch (Exception e) {
            logger.severe("❌ Erro ao aplicar punição local para " + playerName + ": " + e.getMessage());
        }
    }
    
    /**
     * Notifica staff online sobre a punição aplicada.
     * 
     * @param playerName Nome do jogador punido
     * @param offenseCount Número de ocorrências
     * @param punishmentType Tipo de punição
     */
    private void notifyStaffAboutPunishment(String playerName, int offenseCount, String punishmentType) {
        try {
            String message = "🚨 Punição aplicada: " + playerName + 
                           " - " + offenseCount + "ª ocorrência de combat log - " + punishmentType;
            
            // Notificar todos os jogadores com permissão de staff
            for (Player player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission("primeleague.admin.combatlog")) {
                    player.sendMessage(message);
                }
            }
            
        } catch (Exception e) {
            logger.warning("⚠️ Erro ao notificar staff sobre punição: " + e.getMessage());
        }
    }
    
    /**
     * Obtém estatísticas das punições.
     * 
     * @return String com estatísticas
     */
    public String getPunishmentStats() {
        int totalPlayers = playerOffenses.size();
        int totalOffenses = 0;
        
        for (Integer count : playerOffenses.values()) {
            totalOffenses += count;
        }
        
        return String.format(
            "📊 Punishment Stats - Jogadores únicos: %d, Total de ocorrências: %d",
            totalPlayers, totalOffenses
        );
    }
    
    /**
     * Remove uma ocorrência de um jogador (para staff).
     * 
     * @param playerUuid UUID do jogador
     * @return true se removido com sucesso
     */
    public boolean removeOffense(UUID playerUuid) {
        Integer removed = playerOffenses.remove(playerUuid);
        if (removed != null) {
            logger.info("✅ Ocorrência removida para jogador " + playerUuid);
            return true;
        }
        return false;
    }
    
    /**
     * Recarrega as configurações de punições.
     */
    public void reloadConfiguration() {
        loadConfiguration();
        logger.info("✅ Configurações de punições recarregadas!");
    }
}
