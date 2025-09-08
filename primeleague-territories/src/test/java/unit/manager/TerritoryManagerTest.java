package unit.manager;

import br.com.primeleague.api.ClanService;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.model.TerritoryChunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testes Unitários - TerritoryManager")
class TerritoryManagerTest {

    @Mock private Plugin mockPlugin;
    @Mock private BukkitScheduler mockScheduler;
    @Mock private MySqlTerritoryDAO mockDAO;
    @Mock private ClanService mockClanService;
    @Mock private Player mockPlayer;
    @Mock private Location mockLocation;
    @Mock private World mockWorld;
    @Mock private Chunk mockChunk;
    
    private TerritoryManager territoryManager;
    private ClanDTO testClan;
    private TerritoryChunk testTerritory;

    @BeforeEach
    void setUp() {
        // Configurar clã de teste
        testClan = new ClanDTO();
        testClan.setId(1);
        testClan.setTag("TEST");
        testClan.setName("Test Clan");
        
        // Configurar território de teste
        testTerritory = new TerritoryChunk();
        testTerritory.setId(1);
        testTerritory.setClanId(1);
        testTerritory.setWorldName("world");
        testTerritory.setChunkX(0);
        testTerritory.setChunkZ(0);
        
        // Configurar plugin mock básico
        when(mockPlugin.getConfig()).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration.class));
        
        // Criar TerritoryManager com injeção de dependência
        territoryManager = new TerritoryManager(
            mockPlugin,
            mockScheduler,
            mockDAO,
            mockClanService,
            5,    // maxTerritoriesPerClan
            100.0, // maintenanceBaseCost
            1.0,   // maintenanceScale
            24     // maintenanceIntervalHours
        );
    }

    @Test
    @Order(1)
    @DisplayName("Deve verificar se território está reivindicado")
    void testIsClaimed() {
        // Arrange
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        // Act
        boolean result = territoryManager.isClaimed(mockLocation);
        
        // Assert
        assertFalse(result); // Cache vazio inicialmente
    }

    @Test
    @Order(2)
    @DisplayName("Deve obter clã proprietário do território")
    void testGetOwningClan() {
        // Arrange
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        // Act
        ClanDTO result = territoryManager.getOwningClan(mockLocation);
        
        // Assert
        assertNull(result); // Cache vazio inicialmente
    }

    @Test
    @Order(3)
    @DisplayName("Deve obter território em localização")
    void testGetTerritoryAt() {
        // Arrange
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        // Act
        TerritoryChunk result = territoryManager.getTerritoryAt(mockLocation);
        
        // Assert
        assertNull(result); // Cache vazio inicialmente
    }

    @Test
    @Order(4)
    @DisplayName("Deve verificar permissão de território")
    void testHasTerritoryPermission() {
        // Arrange
        when(mockPlayer.getUniqueId()).thenReturn(UUID.randomUUID());
        when(mockClanService.getPlayerClan(any(UUID.class))).thenReturn(testClan);
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        // Act
        boolean result = territoryManager.hasTerritoryPermission(mockPlayer, mockLocation);
        
        // Assert
        assertTrue(result); // Território neutro (cache vazio) = permissão concedida
    }

    @Test
    @Order(5)
    @DisplayName("Deve calcular custo de manutenção")
    void testGetMaintenanceCost() {
        // Act
        double result = territoryManager.getMaintenanceCost(1);
        
        // Assert
        assertEquals(100.0, result, 0.01); // Base cost
    }

    @Test
    @Order(6)
    @DisplayName("Deve verificar se clã está vulnerável")
    void testIsClanVulnerable() {
        // Act
        boolean result = territoryManager.isClanVulnerable(1);
        
        // Assert
        assertFalse(result); // Implementação padrão retorna false
    }
}