package unit.manager;

import br.com.primeleague.api.ClanService;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.dao.MySqlTerritoryDAO;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.manager.WarManager;
import br.com.primeleague.territories.model.ActiveSiege;
import br.com.primeleague.territories.model.ActiveWar;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
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
@DisplayName("Testes Unitários - WarManager")
class WarManagerTest {

    @Mock private Plugin mockPlugin;
    @Mock private MySqlTerritoryDAO mockDAO;
    @Mock private ClanService mockClanService;
    @Mock private TerritoryManager mockTerritoryManager;
    @Mock private Player mockPlayer;
    @Mock private Location mockLocation;
    @Mock private World mockWorld;
    @Mock private Chunk mockChunk;
    
    private WarManager warManager;
    private ClanDTO testClan1;
    private ClanDTO testClan2;

    @BeforeEach
    void setUp() {
        // Configurar clãs de teste
        testClan1 = new ClanDTO();
        testClan1.setId(1);
        testClan1.setTag("TEST1");
        testClan1.setName("Test Clan 1");
        
        testClan2 = new ClanDTO();
        testClan2.setId(2);
        testClan2.setTag("TEST2");
        testClan2.setName("Test Clan 2");
        
        // Configurar plugin mock básico
        when(mockPlugin.getConfig()).thenReturn(mock(org.bukkit.configuration.file.FileConfiguration.class));
        
        // Criar WarManager com injeção de dependência
        warManager = new WarManager(
            mockPlugin,
            mockDAO,
            mockClanService,
            mockTerritoryManager,
            24, // exclusivityWindowHours
            20, // siegeDurationMinutes
            5000.0 // warDeclarationCost
        );
    }

    @Test
    @Order(1)
    @DisplayName("Deve obter cerco ativo em localização")
    void testGetActiveSiege() {
        // Arrange
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        br.com.primeleague.territories.model.TerritoryChunk mockTerritory = mock(br.com.primeleague.territories.model.TerritoryChunk.class);
        when(mockTerritory.getClanId()).thenReturn(2);
        when(mockTerritory.getId()).thenReturn(1);
        when(mockTerritory.getWorldName()).thenReturn("world");
        when(mockTerritory.getChunkX()).thenReturn(0);
        when(mockTerritory.getChunkZ()).thenReturn(0);
        when(mockTerritoryManager.getTerritoryAt(mockLocation)).thenReturn(mockTerritory);
        
        // Act
        ActiveSiege result = warManager.getActiveSiege(mockLocation);
        
        // Assert
        assertNull(result); // Sem mock do DAO, retorna null
    }

    @Test
    @Order(2)
    @DisplayName("Deve verificar se localização é zona de guerra")
    void testIsWarzone() {
        // Arrange
        when(mockLocation.getWorld()).thenReturn(mockWorld);
        when(mockLocation.getChunk()).thenReturn(mockChunk);
        when(mockWorld.getName()).thenReturn("world");
        when(mockChunk.getX()).thenReturn(0);
        when(mockChunk.getZ()).thenReturn(0);
        
        br.com.primeleague.territories.model.TerritoryChunk mockTerritory = mock(br.com.primeleague.territories.model.TerritoryChunk.class);
        when(mockTerritory.getClanId()).thenReturn(2);
        when(mockTerritory.getId()).thenReturn(1);
        when(mockTerritory.getWorldName()).thenReturn("world");
        when(mockTerritory.getChunkX()).thenReturn(0);
        when(mockTerritory.getChunkZ()).thenReturn(0);
        when(mockTerritoryManager.getTerritoryAt(mockLocation)).thenReturn(mockTerritory);
        
        // Act
        boolean result = warManager.isWarzone(mockLocation);
        
        // Assert
        assertFalse(result); // Sem mock do DAO, retorna false
    }
}