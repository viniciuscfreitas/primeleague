package integration;

import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.model.TerritoryChunk;
import br.com.primeleague.territories.model.ClanBank;
import br.com.primeleague.territories.model.ActiveWar;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.core.PrimeLeagueCore;
import br.com.primeleague.core.managers.DataManager;
import com.zaxxer.hikari.HikariDataSource;
import com.zaxxer.hikari.HikariConfig;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.plugin.Plugin;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.List;
import java.util.function.Consumer;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

/**
 * Testes de Integração para MySqlTerritoryDAO.
 * 
 * Seguindo a diretiva arquitetural da IA Arquiteta:
 * - Testa integração real com banco de dados H2 em memória
 * - Valida operações CRUD completas
 * - Verifica transações e consistência de dados
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MySqlTerritoryDAOIntegrationTest {

    @Mock
    private PrimeLeagueCore mockCore;
    
    @Mock
    private DataManager mockDataManager;
    
    @Mock
    private Server mockServer;
    
    @Mock
    private BukkitScheduler mockScheduler;
    
    @Mock
    private Plugin mockPlugin;
    
    private HikariDataSource dataSource;
    private MySqlTerritoryDAO territoryDAO;

    @BeforeEach
    void setUp() throws Exception {
        // Configurar H2 em memória
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1;DB_CLOSE_ON_EXIT=FALSE");
        config.setUsername("sa");
        config.setPassword("");
        config.setMaximumPoolSize(1);
        
        dataSource = new HikariDataSource(config);
        
        // Configurar mocks
        when(mockCore.getDataManager()).thenReturn(mockDataManager);
        when(mockDataManager.getDataSource()).thenReturn(dataSource);
        when(mockCore.getServer()).thenReturn(mockServer);
        when(mockServer.getScheduler()).thenReturn(mockScheduler);
        when(mockCore.getLogger()).thenReturn(mock(org.bukkit.plugin.PluginLogger.class));
        
        // Configurar scheduler mock para executar tasks imediatamente
        doAnswer(invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(mockScheduler).runTaskAsynchronously(any(Plugin.class), any(Runnable.class));
        
        // Criar DAO
        territoryDAO = new MySqlTerritoryDAO(mockCore);
        
        // Criar tabelas de teste
        createTestTables();
    }

    @AfterEach
    void tearDown() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
        }
    }

    private void createTestTables() throws Exception {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Criar tabela de clãs
            stmt.execute("CREATE TABLE IF NOT EXISTS clans (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "tag VARCHAR(10) NOT NULL UNIQUE, " +
                "name VARCHAR(50) NOT NULL, " +
                "creation_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ")");
            
            // Criar tabela de territórios
            stmt.execute("CREATE TABLE IF NOT EXISTS prime_territories (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "clan_id INT NOT NULL, " +
                "world_name VARCHAR(50) NOT NULL, " +
                "chunk_x INT NOT NULL, " +
                "chunk_z INT NOT NULL, " +
                "claimed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "UNIQUE KEY unique_chunk (world_name, chunk_x, chunk_z)" +
                ")");
            
            // Criar tabela de guerras ativas
            stmt.execute("CREATE TABLE IF NOT EXISTS prime_active_wars (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "aggressor_clan_id INT NOT NULL, " +
                "defender_clan_id INT NOT NULL, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time_exclusivity TIMESTAMP NOT NULL" +
                ")");
            
            // Criar tabela de cercos ativos
            stmt.execute("CREATE TABLE IF NOT EXISTS prime_active_sieges (" +
                "id INT PRIMARY KEY AUTO_INCREMENT, " +
                "war_id INT NOT NULL, " +
                "territory_id INT NOT NULL, " +
                "aggressor_clan_id INT NOT NULL, " +
                "defender_clan_id INT NOT NULL, " +
                "start_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "end_time TIMESTAMP NOT NULL, " +
                "altar_location VARCHAR(100) NOT NULL, " +
                "current_timer INT NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'ACTIVE'" +
                ")");
            
            // Criar tabela de banco do clã
            stmt.execute("CREATE TABLE IF NOT EXISTS prime_clan_bank (" +
                "clan_id INT PRIMARY KEY, " +
                "balance DECIMAL(15,2) DEFAULT 0.00" +
                ")");
            
            // Inserir dados de teste
            stmt.execute("INSERT INTO clans (id, tag, name) VALUES (1, 'TEST', 'Test Clan')");
            stmt.execute("INSERT INTO clans (id, tag, name) VALUES (2, 'ENEMY', 'Enemy Clan')");
            stmt.execute("INSERT INTO prime_clan_bank (clan_id, balance) VALUES (1, 1000.00)");
            stmt.execute("INSERT INTO prime_clan_bank (clan_id, balance) VALUES (2, 500.00)");
        }
    }

    @Test
    @Order(1)
    @DisplayName("Deve criar território com sucesso")
    void testCreateTerritory_Success() {
        // Arrange
        TerritoryChunk territory = new TerritoryChunk();
        territory.setClanId(1);
        territory.setWorldName("world");
        territory.setChunkX(0);
        territory.setChunkZ(0);
        territory.setClaimedAt(new Timestamp(System.currentTimeMillis()));
        
        // Act
        boolean[] result = {false};
        territoryDAO.createTerritoryAsync(territory, success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
        assertThat(territory.getId()).isGreaterThan(0);
    }

    @Test
    @Order(2)
    @DisplayName("Deve buscar território por localização")
    void testGetTerritoryByLocation() {
        // Arrange
        TerritoryChunk territory = new TerritoryChunk();
        territory.setClanId(1);
        territory.setWorldName("world");
        territory.setChunkX(10);
        territory.setChunkZ(20);
        territory.setClaimedAt(new Timestamp(System.currentTimeMillis()));
        
        // Criar território primeiro
        boolean[] created = {false};
        territoryDAO.createTerritoryAsync(territory, success -> created[0] = success);
        assertThat(created[0]).isTrue();
        
        // Act
        TerritoryChunk[] found = {null};
        territoryDAO.getTerritoryByLocationAsync("world", 10, 20, result -> found[0] = result);
        
        // Assert
        assertThat(found[0]).isNotNull();
        assertThat(found[0].getClanId()).isEqualTo(1);
        assertThat(found[0].getChunkX()).isEqualTo(10);
        assertThat(found[0].getChunkZ()).isEqualTo(20);
    }

    @Test
    @Order(3)
    @DisplayName("Deve buscar territórios por clã")
    void testGetTerritoriesByClan() {
        // Arrange
        TerritoryChunk territory1 = new TerritoryChunk();
        territory1.setClanId(1);
        territory1.setWorldName("world");
        territory1.setChunkX(0);
        territory1.setChunkZ(0);
        territory1.setClaimedAt(new Timestamp(System.currentTimeMillis()));
        
        TerritoryChunk territory2 = new TerritoryChunk();
        territory2.setClanId(1);
        territory2.setWorldName("world");
        territory2.setChunkX(1);
        territory2.setChunkZ(1);
        territory2.setClaimedAt(new Timestamp(System.currentTimeMillis()));
        
        // Criar territórios
        territoryDAO.createTerritoryAsync(territory1, success -> {});
        territoryDAO.createTerritoryAsync(territory2, success -> {});
        
        // Act
        List<TerritoryChunk>[] territories = new List[1];
        territoryDAO.getTerritoriesByClanAsync(1, result -> territories[0] = result);
        
        // Assert
        assertThat(territories[0]).hasSize(2);
        assertThat(territories[0]).allMatch(t -> t.getClanId() == 1);
    }

    @Test
    @Order(4)
    @DisplayName("Deve remover território com sucesso")
    void testRemoveTerritory() {
        // Arrange
        TerritoryChunk territory = new TerritoryChunk();
        territory.setClanId(1);
        territory.setWorldName("world");
        territory.setChunkX(5);
        territory.setChunkZ(5);
        territory.setClaimedAt(new Timestamp(System.currentTimeMillis()));
        
        // Criar território primeiro
        boolean[] created = {false};
        territoryDAO.createTerritoryAsync(territory, success -> created[0] = success);
        assertThat(created[0]).isTrue();
        
        // Act
        boolean[] removed = {false};
        territoryDAO.removeTerritoryAsync(territory.getId(), success -> removed[0] = success);
        
        // Assert
        assertThat(removed[0]).isTrue();
        
        // Verificar se foi removido
        TerritoryChunk[] found = {null};
        territoryDAO.getTerritoryByLocationAsync("world", 5, 5, result -> found[0] = result);
        assertThat(found[0]).isNull();
    }

    @Test
    @Order(5)
    @DisplayName("Deve criar guerra ativa com sucesso")
    void testCreateActiveWar() {
        // Arrange
        ActiveWar war = new ActiveWar();
        war.setAggressorClanId(1);
        war.setDefenderClanId(2);
        war.setStartTime(new Timestamp(System.currentTimeMillis()));
        war.setEndTimeExclusivity(new Timestamp(System.currentTimeMillis() + 86400000)); // 24 horas
        
        // Act
        boolean[] result = {false};
        territoryDAO.createActiveWarAsync(war, success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
        assertThat(war.getId()).isGreaterThan(0);
    }

    @Test
    @Order(6)
    @DisplayName("Deve criar cerco ativo com sucesso")
    void testCreateActiveSiege() {
        // Arrange
        ActiveSiege siege = new ActiveSiege();
        siege.setWarId(1);
        siege.setTerritoryId(1);
        siege.setAggressorClanId(1);
        siege.setDefenderClanId(2);
        siege.setStartTime(new Timestamp(System.currentTimeMillis()));
        siege.setEndTime(new Timestamp(System.currentTimeMillis() + 1200000)); // 20 minutos
        // Mock Location para ActiveSiege
        org.bukkit.Location mockAltarLocation = mock(org.bukkit.Location.class);
        org.bukkit.World mockAltarWorld = mock(org.bukkit.World.class);
        when(mockAltarLocation.getWorld()).thenReturn(mockAltarWorld);
        when(mockAltarWorld.getName()).thenReturn("world");
        when(mockAltarLocation.getBlockX()).thenReturn(100);
        when(mockAltarLocation.getBlockY()).thenReturn(64);
        when(mockAltarLocation.getBlockZ()).thenReturn(200);
        siege.setAltarLocation(mockAltarLocation);
        siege.setRemainingTime(1200); // 20 minutos em segundos
        
        // Act
        boolean[] result = {false};
        territoryDAO.createActiveSiegeAsync(siege, success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
        assertThat(siege.getId()).isGreaterThan(0);
    }

    @Test
    @Order(7)
    @DisplayName("Deve atualizar cerco ativo com sucesso")
    void testUpdateActiveSiege() {
        // Arrange
        ActiveSiege siege = new ActiveSiege();
        siege.setWarId(1);
        siege.setTerritoryId(1);
        siege.setAggressorClanId(1);
        siege.setDefenderClanId(2);
        siege.setStartTime(new Timestamp(System.currentTimeMillis()));
        siege.setEndTime(new Timestamp(System.currentTimeMillis() + 1200000));
        // Mock Location para ActiveSiege
        org.bukkit.Location mockAltarLocation2 = mock(org.bukkit.Location.class);
        org.bukkit.World mockAltarWorld2 = mock(org.bukkit.World.class);
        when(mockAltarLocation2.getWorld()).thenReturn(mockAltarWorld2);
        when(mockAltarWorld2.getName()).thenReturn("world");
        when(mockAltarLocation2.getBlockX()).thenReturn(100);
        when(mockAltarLocation2.getBlockY()).thenReturn(64);
        when(mockAltarLocation2.getBlockZ()).thenReturn(200);
        siege.setAltarLocation(mockAltarLocation2);
        siege.setRemainingTime(1200);
        
        // Criar cerco primeiro
        territoryDAO.createActiveSiegeAsync(siege, success -> {});
        
        // Modificar cerco
        siege.setRemainingTime(600); // 10 minutos restantes
        siege.setEndTime(new Timestamp(System.currentTimeMillis() + 600000));
        
        // Act
        boolean[] result = {false};
        territoryDAO.updateActiveSiegeAsync(siege, success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
    }

    @Test
    @Order(8)
    @DisplayName("Deve obter banco do clã com sucesso")
    void testGetClanBank() {
        // Act
        ClanBank[] bank = {null};
        territoryDAO.getClanBankAsync(1, result -> bank[0] = result);
        
        // Assert
        assertThat(bank[0]).isNotNull();
        assertThat(bank[0].getClanId()).isEqualTo(1);
        assertThat(bank[0].getBalance()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }

    @Test
    @Order(9)
    @DisplayName("Deve depositar no banco do clã com sucesso")
    void testDepositToClanBank() {
        // Act
        boolean[] result = {false};
        territoryDAO.depositToClanBankAsync(1, new BigDecimal("500.00"), success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
        
        // Verificar saldo atualizado
        ClanBank[] bank = {null};
        territoryDAO.getClanBankAsync(1, b -> bank[0] = b);
        assertThat(bank[0].getBalance()).isEqualByComparingTo(new BigDecimal("1500.00"));
    }

    @Test
    @Order(10)
    @DisplayName("Deve sacar do banco do clã com sucesso")
    void testWithdrawFromClanBank() {
        // Act
        boolean[] result = {false};
        territoryDAO.withdrawFromClanBankAsync(1, new BigDecimal("200.00"), success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isTrue();
        
        // Verificar saldo atualizado
        ClanBank[] bank = {null};
        territoryDAO.getClanBankAsync(1, b -> bank[0] = b);
        assertThat(bank[0].getBalance()).isEqualByComparingTo(new BigDecimal("800.00"));
    }

    @Test
    @Order(11)
    @DisplayName("Deve falhar ao sacar saldo insuficiente")
    void testWithdrawInsufficientBalance() {
        // Act
        boolean[] result = {false};
        territoryDAO.withdrawFromClanBankAsync(2, new BigDecimal("1000.00"), success -> result[0] = success);
        
        // Assert
        assertThat(result[0]).isFalse();
        
        // Verificar que saldo não mudou
        ClanBank[] bank = {null};
        territoryDAO.getClanBankAsync(2, b -> bank[0] = b);
        assertThat(bank[0].getBalance()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    @Order(12)
    @DisplayName("Deve criar banco para clã inexistente")
    void testGetClanBankForNonExistentClan() {
        // Act
        ClanBank[] bank = {null};
        territoryDAO.getClanBankAsync(999, result -> bank[0] = result);
        
        // Assert
        assertThat(bank[0]).isNotNull();
        assertThat(bank[0].getClanId()).isEqualTo(999);
        assertThat(bank[0].getBalance()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}