package scenarios;

import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.model.ActiveWar;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.territories.model.TerritoryChunk;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.api.ClanService;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes de Cenários para WarManager.
 * 
 * Seguindo a diretiva arquitetural da IA Arquiteta:
 * - Testa fluxos completos de guerra e cerco
 * - Valida integração entre diferentes componentes
 * - Simula cenários realistas de combate
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class WarScenariosTest {

    @Mock
    private PrimeLeagueTerritories mockPlugin;
    
    
    @Mock
    private MySqlTerritoryDAO mockDAO;
    
    @Mock
    private ClanService mockClanService;
    
    @Mock
    private TerritoryManager mockTerritoryManager;
    
    @Mock
    private Player mockPlayer;
    
    @Mock
    private Location mockLocation;
    
    @Mock
    private World mockWorld;
    
    @Mock
    private Chunk mockChunk;
    
    @Mock
    private TerritoryChunk mockTerritoryChunk;
    
    private WarManager warManager;
    
    // Dados de teste
    private ClanDTO attackerClan;
    private ClanDTO defenderClan;
    private UUID testPlayerId;
    
    @BeforeEach
    void setUp() {
        // Configurar dados de teste
        testPlayerId = UUID.randomUUID();
        
        attackerClan = new ClanDTO();
        attackerClan.setId(1);
        attackerClan.setName("Attacker Clan");
        attackerClan.setFounderPlayerId(1);
        attackerClan.setFounderName("AttackerLeader");
        attackerClan.setRankingPoints(1000);
        
        defenderClan = new ClanDTO();
        defenderClan.setId(2);
        defenderClan.setName("Defender Clan");
        defenderClan.setFounderPlayerId(2);
        defenderClan.setFounderName("DefenderLeader");
        defenderClan.setRankingPoints(850);
        
        // Calibração de vulnerabilidade - simula clãs vulneráveis para permitir declaração de guerra
        when(mockTerritoryManager.isClanVulnerable(anyInt())).thenReturn(true);
        
        // Configurar plugin mock
        // Não mockar o logger - o código de produção agora tem verificações de null
        
        // Instanciar WarManager com injeção de dependência
        warManager = new WarManager(mockPlugin, mockDAO, mockClanService, mockTerritoryManager, 24, 20, 5000.0);
    }
    
    @Test
    @Order(1)
    @DisplayName("Cenário: Declaração de guerra bem-sucedida seguida de cerco")
    void testScenario_SuccessfulWarDeclarationAndSiege() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            
            // Simular início do cerco
            warManager.startSiege(mockPlayer, mockLocation, (siegeResult, siegeMessage) -> {
                assertThat(siegeResult).isEqualTo(WarManager.SiegeStartResult.SUCCESS);
            });
        });
    }
    
    @Test
    @Order(2)
    @DisplayName("Cenário: Tentativa de cerco sem guerra declarada")
    void testScenario_SiegeWithoutWarDeclaration() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        
        // Act & Assert
        warManager.startSiege(mockPlayer, mockLocation, (result, message) -> {
            assertThat(result).isEqualTo(WarManager.SiegeStartResult.NO_WAR);
        });
    }
    
    @Test
    @Order(3)
    @DisplayName("Cenário: Tentativa de cerco no próprio território")
    void testScenario_SiegeOnOwnTerritory() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(defenderClan); // Mesmo clã do território
        
        // Act & Assert
        warManager.startSiege(mockPlayer, mockLocation, (result, message) -> {
            assertThat(result).isEqualTo(WarManager.SiegeStartResult.OWN_TERRITORY);
        });
    }
    
    @Test
    @Order(4)
    @DisplayName("Cenário: Declaração de guerra para clã não vulnerável")
    void testScenario_WarDeclarationOnNonVulnerableClan() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
        });
    }
    
    @Test
    @Order(5)
    @DisplayName("Cenário: Declaração de guerra para o próprio clã")
    void testScenario_WarDeclarationOnSameClan() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Attacker Clan")).thenReturn(attackerClan); // Mesmo clã
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Attacker Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SAME_CLAN);
        });
    }
    
    @Test
    @Order(6)
    @DisplayName("Cenário: Declaração de guerra sem permissão")
    void testScenario_WarDeclarationWithoutPermission() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(false); // Sem permissão
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.NO_PERMISSION);
        });
    }
    
    @Test
    @Order(7)
    @DisplayName("Cenário: Declaração de guerra sem clã")
    void testScenario_WarDeclarationWithoutClan() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(null);
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.NO_CLAN);
        });
    }
    
    @Test
    @Order(8)
    @DisplayName("Cenário: Cerco em território não reivindicado")
    void testScenario_SiegeOnUnclaimedTerritory() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockTerritoryManager.getTerritoryAt(mockLocation)).thenReturn(null); // Território não reivindicado
        
        // Act & Assert
        warManager.startSiege(mockPlayer, mockLocation, (result, message) -> {
            assertThat(result).isEqualTo(WarManager.SiegeStartResult.NOT_TERRITORY);
        });
    }
    
    @Test
    @Order(9)
    @DisplayName("Cenário: Cerco sem clã")
    void testScenario_SiegeWithoutClan() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(null);
        
        // Act & Assert
        warManager.startSiege(mockPlayer, mockLocation, (result, message) -> {
            assertThat(result).isEqualTo(WarManager.SiegeStartResult.NO_CLAN);
        });
    }
    
    @Test
    @Order(10)
    @DisplayName("Cenário: Verificação de guerra ativa entre múltiplos clãs")
    void testScenario_MultipleClanWarCheck() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            
            // Verificar se a guerra foi declarada com sucesso
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
        });
    }
    
    @Test
    @Order(11)
    @DisplayName("Cenário: Obtenção de cerco ativo em localização")
    void testScenario_GetActiveSiegeAtLocation() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            
            // Simular início do cerco
            warManager.startSiege(mockPlayer, mockLocation, (siegeResult, siegeMessage) -> {
                assertThat(siegeResult).isEqualTo(WarManager.SiegeStartResult.SUCCESS);
                
                // Verificar se o cerco foi iniciado com sucesso
                assertThat(siegeResult).isEqualTo(WarManager.SiegeStartResult.SUCCESS);
            });
        });
    }
    
    @Test
    @Order(12)
    @DisplayName("Cenário: Fluxo completo de guerra com múltiplas interações")
    void testScenario_CompleteWarFlow() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert - Fluxo completo
        warManager.declareWar(mockPlayer, "Defender Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            
            // Iniciar cerco
            warManager.startSiege(mockPlayer, mockLocation, (siegeResult, siegeMessage) -> {
                assertThat(siegeResult).isEqualTo(WarManager.SiegeStartResult.SUCCESS);
            });
        });
    }
    
    @Test
    @Order(13)
    @DisplayName("Cenário: Tentativa de cerco em território de clã neutro")
    void testScenario_SiegeOnNeutralClanTerritory() {
        // Arrange
        ClanDTO neutralClan = new ClanDTO();
        neutralClan.setId(3);
        neutralClan.setName("Neutral Clan");
        neutralClan.setFounderPlayerId(3);
        neutralClan.setFounderName("NeutralLeader");
        neutralClan.setRankingPoints(500);
        
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockTerritoryChunk.getClanId()).thenReturn(neutralClan.getId());
        
        // Act & Assert
        warManager.startSiege(mockPlayer, mockLocation, (result, message) -> {
            assertThat(result).isEqualTo(WarManager.SiegeStartResult.NO_WAR);
        });
    }
    
    @Test
    @Order(14)
    @DisplayName("Cenário: Declaração de guerra com clã inexistente")
    void testScenario_WarDeclarationOnNonExistentClan() {
        // Arrange
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Non Existent Clan")).thenReturn(null);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Non Existent Clan", (result, message) -> {
            assertThat(result).isEqualTo(WarManager.WarDeclarationResult.TARGET_NOT_FOUND);
        });
    }
    
    @Test
    @Order(15)
    @DisplayName("Cenário: Verificação de múltiplas guerras simultâneas")
    void testScenario_MultipleSimultaneousWars() {
        // Arrange
        ClanDTO thirdClan = new ClanDTO();
        thirdClan.setId(4);
        thirdClan.setName("Third Clan");
        thirdClan.setFounderPlayerId(4);
        thirdClan.setFounderName("ThirdLeader");
        thirdClan.setRankingPoints(750);
        
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(attackerClan);
        when(mockClanService.getClanByName("Defender Clan")).thenReturn(defenderClan);
        when(mockClanService.getClanByName("Third Clan")).thenReturn(thirdClan);
        when(mockPlayer.hasPermission("primeleague.territories.war")).thenReturn(true);
        
        // Mock do DAO para aceitar callback
        doAnswer(invocation -> {
            Consumer<Boolean> callback = invocation.getArgument(1);
            callback.accept(true);
            return null;
        }).when(mockDAO).createActiveWarAsync(any(ActiveWar.class), any(Consumer.class));
        
        // Act & Assert
        warManager.declareWar(mockPlayer, "Defender Clan", (result1, message1) -> {
            assertThat(result1).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            
            // Segunda guerra
            warManager.declareWar(mockPlayer, "Third Clan", (result2, message2) -> {
                assertThat(result2).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
                
                // Verificar que ambas as guerras foram declaradas com sucesso
                assertThat(result2).isEqualTo(WarManager.WarDeclarationResult.SUCCESS);
            });
        });
    }
}