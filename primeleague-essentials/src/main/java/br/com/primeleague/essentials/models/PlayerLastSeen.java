package br.com.primeleague.essentials.models;

import java.sql.Timestamp;

/**
 * Modelo de dados para informações de última vez visto de um jogador.
 * Usado pelo comando /seen para exibir quando um jogador foi visto pela última vez.
 * 
 * @author PrimeLeague Development Team
 * @version 1.0.0
 */
public class PlayerLastSeen {
    
    private String playerName;
    private String uuid;
    private Timestamp lastSeen;
    private boolean isOnline;
    private String formattedTime;
    
    /**
     * Construtor para jogador online.
     */
    public PlayerLastSeen(String playerName, String uuid, boolean isOnline) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.isOnline = isOnline;
        this.lastSeen = null;
        this.formattedTime = null;
    }
    
    /**
     * Construtor para jogador offline.
     */
    public PlayerLastSeen(String playerName, String uuid, Timestamp lastSeen, boolean isOnline) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.lastSeen = lastSeen;
        this.isOnline = isOnline;
        this.formattedTime = null;
    }
    
    /**
     * Construtor completo.
     */
    public PlayerLastSeen(String playerName, String uuid, Timestamp lastSeen, boolean isOnline, String formattedTime) {
        this.playerName = playerName;
        this.uuid = uuid;
        this.lastSeen = lastSeen;
        this.isOnline = isOnline;
        this.formattedTime = formattedTime;
    }
    
    /**
     * Verifica se o jogador nunca foi visto.
     */
    public boolean neverSeen() {
        return lastSeen == null && !isOnline;
    }
    
    /**
     * Obtém o tempo relativo formatado.
     */
    public String getFormattedRelativeTime() {
        if (isOnline) {
            return "online agora";
        }
        
        if (neverSeen()) {
            return "nunca foi visto";
        }
        
        if (formattedTime != null) {
            return formattedTime;
        }
        
        // Formatação básica se não foi definida
        long timeDiff = System.currentTimeMillis() - lastSeen.getTime();
        return formatTimeDifference(timeDiff);
    }
    
    /**
     * Formata a diferença de tempo em formato amigável.
     */
    private String formatTimeDifference(long timeDiff) {
        long seconds = timeDiff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return "há " + days + " dia" + (days > 1 ? "s" : "");
        } else if (hours > 0) {
            return "há " + hours + " hora" + (hours > 1 ? "s" : "");
        } else if (minutes > 0) {
            return "há " + minutes + " minuto" + (minutes > 1 ? "s" : "");
        } else {
            return "há " + seconds + " segundo" + (seconds > 1 ? "s" : "");
        }
    }
    
    // Getters e Setters
    public String getPlayerName() { return playerName; }
    public void setPlayerName(String playerName) { this.playerName = playerName; }
    
    public String getUuid() { return uuid; }
    public void setUuid(String uuid) { this.uuid = uuid; }
    
    public Timestamp getLastSeen() { return lastSeen; }
    public void setLastSeen(Timestamp lastSeen) { this.lastSeen = lastSeen; }
    
    public boolean isOnline() { return isOnline; }
    public void setOnline(boolean online) { isOnline = online; }
    
    public String getFormattedTime() { return formattedTime; }
    public void setFormattedTime(String formattedTime) { this.formattedTime = formattedTime; }
    
    @Override
    public String toString() {
        return "PlayerLastSeen{" +
                "playerName='" + playerName + '\'' +
                ", uuid='" + uuid + '\'' +
                ", lastSeen=" + lastSeen +
                ", isOnline=" + isOnline +
                ", formattedTime='" + formattedTime + '\'' +
                '}';
    }
}
