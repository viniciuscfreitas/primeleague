package br.com.primeleague.clans.model;

import java.util.Set;
import java.util.HashSet;
import java.util.UUID; // Usaremos UUID para o DB, mas nomes para a lógica em tempo de execução 1.5.2

/**
 * Representa uma organização de jogadores (Clã) no Prime League.
 * Esta classe é um objeto de dados (POJO) e não contém lógica de negócios complexa.
 *
 * @version 1.0
 * @author PrimeLeague Team
 */
public final class Clan {

    private final int id; // ID do banco de dados
    private final String tag;
    private String name;

    // Na 1.5.2, é mais prático e performático gerenciar jogadores online por seus nomes.
    private String founderName;
    private final Set<String> officers;
    private final Set<String> members;
    private boolean friendlyFireEnabled;
    private int penaltyPoints; // Pontos de penalidade acumulados pelo clã
    private int rankingPoints; // Pontos de ranking do clã

    /**
     * Construtor para criar um novo clã.
     *
     * @param id O ID único do clã, geralmente vindo do banco de dados.
     * @param tag A tag única do clã (e.g., "PRL").
     * @param name O nome completo do clã.
     * @param founderName O nome do jogador que fundou o clã.
     */
    public Clan(int id, String tag, String name, String founderName) {
        this(id, tag, name, founderName, 1000); // Usar valor padrão
    }

    /**
     * Construtor para criar um novo clã com pontos iniciais configuráveis.
     *
     * @param id O ID único do clã, geralmente vindo do banco de dados.
     * @param tag A tag única do clã (e.g., "PRL").
     * @param name O nome completo do clã.
     * @param founderName O nome do jogador que fundou o clã.
     * @param initialRankingPoints Pontos iniciais de ranking para o clã.
     */
    public Clan(int id, String tag, String name, String founderName, int initialRankingPoints) {
        this.id = id;
        this.tag = tag;
        this.name = name;
        this.founderName = founderName;
        
        // Usar HashSet para buscas rápidas (O(1) em média).
        this.officers = new HashSet<String>();
        this.members = new HashSet<String>();
        
        // O fundador também é um membro e oficial (hierarquia completa).
        this.members.add(founderName.toLowerCase());
        this.officers.add(founderName.toLowerCase());
        this.friendlyFireEnabled = false; // Friendly fire desabilitado por padrão
        this.penaltyPoints = 0; // Inicialmente sem pontos de penalidade
        this.rankingPoints = initialRankingPoints; // Pontos iniciais de ranking configuráveis
    }

    // --- Getters ---

    public int getId() {
        return id;
    }

    public String getTag() {
        return tag;
    }

    public String getName() {
        return name;
    }

    public String getFounderName() {
        return founderName;
    }

    /**
     * Define o nome do fundador do clã.
     * Usado para transferência de fundador.
     *
     * @param founderName Nome do novo fundador
     */
    public void setFounderName(String founderName) {
        this.founderName = founderName;
    }
    
    public Set<String> getOfficers() {
        return officers;
    }

    public Set<String> getMembers() {
        return members;
    }

    // --- Lógica de Membros ---

    public boolean isFounder(String playerName) {
        return this.founderName.equalsIgnoreCase(playerName);
    }

    public boolean isOfficer(String playerName) {
        return this.officers.contains(playerName.toLowerCase());
    }

    public boolean isMember(String playerName) {
        return this.members.contains(playerName.toLowerCase());
    }

    // --- Métodos de Gerenciamento de Membros ---

    /**
     * Adiciona um membro ao clã.
     *
     * @param playerName Nome do jogador a ser adicionado
     */
    public void addMember(String playerName) {
        this.members.add(playerName.toLowerCase());
    }

    /**
     * Remove um membro do clã.
     *
     * @param playerName Nome do jogador a ser removido
     */
    public void removeMember(String playerName) {
        String lowerCaseName = playerName.toLowerCase();
        this.members.remove(lowerCaseName);
        this.officers.remove(lowerCaseName); // Garante que ele também seja removido de oficial
    }

    /**
     * Promove um membro a oficial.
     *
     * @param playerName Nome do jogador a ser promovido
     */
    public void promoteMember(String playerName) {
        String lowerCaseName = playerName.toLowerCase();
        if (this.members.contains(lowerCaseName)) { // Garante que só membros podem ser promovidos
            this.officers.add(lowerCaseName);
        }
    }

    /**
     * Rebaixa um oficial a membro.
     *
     * @param playerName Nome do jogador a ser rebaixado
     */
    public void demoteMember(String playerName) {
        this.officers.remove(playerName.toLowerCase());
    }

    /**
     * Obtém todos os nomes de membros do clã (líder, oficiais e membros).
     *
     * @return Set com todos os nomes de membros
     */
    public Set<String> getAllMemberNames() {
        Set<String> allMembers = new HashSet<>(this.members);
        allMembers.addAll(this.officers);
        allMembers.add(this.founderName.toLowerCase()); // Garantir que o fundador esteja incluído
        return allMembers;
    }

    // --- Getters e Setters para Friendly Fire ---

    /**
     * Verifica se o friendly fire está habilitado no clã.
     *
     * @return true se o friendly fire está habilitado
     */
    public boolean isFriendlyFireEnabled() {
        return friendlyFireEnabled;
    }

    /**
     * Define se o friendly fire está habilitado no clã.
     *
     * @param friendlyFireEnabled true para habilitar, false para desabilitar
     */
    public void setFriendlyFireEnabled(boolean friendlyFireEnabled) {
        this.friendlyFireEnabled = friendlyFireEnabled;
    }

    // --- Getters e Setters para Pontos de Penalidade ---

    /**
     * Obtém os pontos de penalidade acumulados pelo clã.
     *
     * @return Pontos de penalidade
     */
    public int getPenaltyPoints() {
        return penaltyPoints;
    }

    /**
     * Define os pontos de penalidade do clã.
     *
     * @param penaltyPoints Novos pontos de penalidade
     */
    public void setPenaltyPoints(int penaltyPoints) {
        this.penaltyPoints = penaltyPoints;
    }
    
    // --- Getters e Setters para Pontos de Ranking ---
    
    /**
     * Obtém os pontos de ranking do clã.
     *
     * @return Pontos de ranking
     */
    public int getRankingPoints() {
        return rankingPoints;
    }
    
    /**
     * Define os pontos de ranking do clã.
     *
     * @param rankingPoints Novos pontos de ranking
     */
    public void setRankingPoints(int rankingPoints) {
        this.rankingPoints = rankingPoints;
    }
}
