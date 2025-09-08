package unit.commands;

import br.com.primeleague.territories.commands.TerritoryCommand;
import br.com.primeleague.territories.manager.TerritoryManager;
import br.com.primeleague.territories.util.MessageManager;
import br.com.primeleague.territories.PrimeLeagueTerritories;
import br.com.primeleague.api.dto.ClanDTO;
import br.com.primeleague.territories.model.TerritoryChunk;
import br.com.primeleague.territories.model.ClanBank;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.Chunk;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;
import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Testes Unitários para TerritoryCommand.
 * 
 * Seguindo a diretiva arquitetural da IA Arquiteta:
 * - Testa a lógica de comandos isolada de dependências externas
 * - Utiliza mocks para todas as dependências (Manager, MessageManager, etc.)
 * - Foca na validação das respostas dos comandos
 * 
 * @author PrimeLeague Team
 * @version 1.0
 */
@ExtendWith(MockitoExtension.class)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class TerritoryCommandTest {

    @Mock
    private PrimeLeagueTerritories mockPlugin;
    
    @Mock
    private TerritoryManager mockTerritoryManager;
    
    @Mock
    private MessageManager mockMessageManager;
    
    @Mock
    private Player mockPlayer;
    
    @Mock
    private CommandSender mockCommandSender;
    
    @Mock
    private Command mockCommand;
    
    @Mock
    private Location mockLocation;
    
    @Mock
    private World mockWorld;
    
    @Mock
    private Chunk mockChunk;
    
    private TerritoryCommand territoryCommand;
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
        
        // Configurar plugin mock
        when(mockPlugin.getTerritoryManager()).thenReturn(mockTerritoryManager);
        when(mockPlugin.getMessageManager()).thenReturn(mockMessageManager);
        
        // Configurar MessageManager mock
        when(mockMessageManager.getMessage(anyString())).thenReturn("Test message");
        when(mockMessageManager.getMessage(anyString(), any())).thenReturn("Test message with placeholders");
        
        // Criar TerritoryCommand com mocks
        territoryCommand = new TerritoryCommand(mockPlugin);
    }

    @Test
    @Order(1)
    @DisplayName("Deve mostrar ajuda quando comando é executado sem argumentos")
    void testOnCommand_NoArgs_ShowsHelp() {
        // Arrange
        String[] args = {};
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("COMANDOS DE TERRITÓRIO"));
    }

    @Test
    @Order(2)
    @DisplayName("Deve rejeitar comando de sender não-jogador")
    void testOnCommand_NonPlayerSender() {
        // Arrange
        String[] args = {"claim"};
        
        // Act
        boolean result = territoryCommand.onCommand(mockCommandSender, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockCommandSender).sendMessage(anyString());
    }

    @Test
    @Order(3)
    @DisplayName("Deve executar comando claim com sucesso")
    void testOnCommand_Claim_Success() {
        // Arrange
        String[] args = {"claim"};
        when(mockPlayer.hasPermission("primeleague.territories.claim")).thenReturn(true);
        
        // Mock do TerritoryManager para aceitar callback
        doAnswer(invocation -> {
            TerritoryManager.TerritoryClaimCallback callback = invocation.getArgument(2);
            callback.onResult(TerritoryManager.TerritoryClaimResult.SUCCESS, "Success");
            return null;
        }).when(mockTerritoryManager).claimTerritory(any(Player.class), any(Location.class), any(TerritoryManager.TerritoryClaimCallback.class));
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockTerritoryManager).claimTerritory(eq(mockPlayer), eq(mockLocation), any(TerritoryManager.TerritoryClaimCallback.class));
    }

    @Test
    @Order(4)
    @DisplayName("Deve falhar comando claim sem permissão")
    void testOnCommand_Claim_NoPermission() {
        // Arrange
        String[] args = {"claim"};
        when(mockPlayer.hasPermission("primeleague.territories.claim")).thenReturn(false);
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(anyString());
        verify(mockTerritoryManager, never()).claimTerritory(any(), any(), any());
    }

    @Test
    @Order(5)
    @DisplayName("Deve executar comando unclaim com sucesso")
    void testOnCommand_Unclaim_Success() {
        // Arrange
        String[] args = {"unclaim"};
        when(mockPlayer.hasPermission("primeleague.territories.unclaim")).thenReturn(true);
        
        // Mock do TerritoryManager para aceitar callback
        doAnswer(invocation -> {
            TerritoryManager.TerritoryUnclaimCallback callback = invocation.getArgument(2);
            callback.onResult(TerritoryManager.TerritoryUnclaimResult.SUCCESS, "Success");
            return null;
        }).when(mockTerritoryManager).unclaimTerritory(any(Player.class), any(Location.class), any(TerritoryManager.TerritoryUnclaimCallback.class));
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockTerritoryManager).unclaimTerritory(eq(mockPlayer), eq(mockLocation), any(TerritoryManager.TerritoryUnclaimCallback.class));
    }

    @Test
    @Order(6)
    @DisplayName("Deve executar comando info com território existente")
    void testOnCommand_Info_WithTerritory() {
        // Arrange
        String[] args = {"info"};
        when(mockTerritoryManager.getTerritoryAt(mockLocation)).thenReturn(testTerritory);
        when(mockTerritoryManager.getOwningClan(mockLocation)).thenReturn(testClan);
        when(mockTerritoryManager.getTerritoryState(1)).thenReturn(br.com.primeleague.territories.model.TerritoryState.FORTIFICADO);
        when(mockTerritoryManager.getTerritoryCount(1)).thenReturn(5);
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("INFORMAÇÕES DO TERRITÓRIO"));
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("TEST"));
    }

    @Test
    @Order(7)
    @DisplayName("Deve executar comando info sem território")
    void testOnCommand_Info_NoTerritory() {
        // Arrange
        String[] args = {"info"};
        when(mockTerritoryManager.getTerritoryAt(mockLocation)).thenReturn(null);
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("não está em um território reivindicado"));
    }

    @Test
    @Order(8)
    @DisplayName("Deve executar comando list com territórios")
    void testOnCommand_List_WithTerritories() {
        // Arrange
        String[] args = {"list"};
        java.util.List<TerritoryChunk> territories = java.util.Arrays.asList(testTerritory);
        when(mockTerritoryManager.getClanTerritories(1)).thenReturn(territories);
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("TERRITÓRIOS DO CLÃ"));
    }

    @Test
    @Order(9)
    @DisplayName("Deve executar comando list sem territórios")
    void testOnCommand_List_NoTerritories() {
        // Arrange
        String[] args = {"list"};
        when(mockTerritoryManager.getClanTerritories(1)).thenReturn(java.util.Collections.emptyList());
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("não possui territórios reivindicados"));
    }

    @Test
    @Order(10)
    @DisplayName("Deve executar comando bank balance com sucesso")
    void testOnCommand_BankBalance_Success() {
        // Arrange
        String[] args = {"bank", "balance"};
        ClanBank mockBank = mock(ClanBank.class);
        when(mockBank.getBalance()).thenReturn(new BigDecimal("1000.00"));
        when(mockTerritoryManager.getMaintenanceCost(1)).thenReturn(100.0);
        
        // Mock do TerritoryManager para aceitar callback
        doAnswer(invocation -> {
            java.util.function.Consumer<ClanBank> callback = invocation.getArgument(1);
            callback.accept(mockBank);
            return null;
        }).when(mockTerritoryManager).getClanBank(anyInt(), any(java.util.function.Consumer.class));
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("BANCO DO CLÃ"));
        verify(mockPlayer, atLeastOnce()).sendMessage(contains("1000.00"));
    }

    @Test
    @Order(11)
    @DisplayName("Deve executar comando bank deposit com sucesso")
    void testOnCommand_BankDeposit_Success() {
        // Arrange
        String[] args = {"bank", "deposit", "500"};
        
        // Mock do TerritoryManager para aceitar callback
        doAnswer(invocation -> {
            java.util.function.Consumer<Boolean> callback = invocation.getArgument(2);
            callback.accept(true);
            return null;
        }).when(mockTerritoryManager).depositToClanBank(anyInt(), any(BigDecimal.class), any(java.util.function.Consumer.class));
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockTerritoryManager).depositToClanBank(eq(1), eq(new BigDecimal("500")), any(java.util.function.Consumer.class));
    }

    @Test
    @Order(12)
    @DisplayName("Deve executar comando bank withdraw com sucesso")
    void testOnCommand_BankWithdraw_Success() {
        // Arrange
        String[] args = {"bank", "withdraw", "200"};
        ClanBank mockBank = mock(ClanBank.class);
        when(mockBank.hasEnoughBalance(any(BigDecimal.class))).thenReturn(true);
        
        // Mock do TerritoryManager para aceitar callbacks
        doAnswer(invocation -> {
            java.util.function.Consumer<ClanBank> callback = invocation.getArgument(1);
            callback.accept(mockBank);
            return null;
        }).when(mockTerritoryManager).getClanBank(anyInt(), any(java.util.function.Consumer.class));
        
        doAnswer(invocation -> {
            java.util.function.Consumer<Boolean> callback = invocation.getArgument(2);
            callback.accept(true);
            return null;
        }).when(mockTerritoryManager).withdrawFromClanBank(anyInt(), any(BigDecimal.class), any(java.util.function.Consumer.class));
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockTerritoryManager).withdrawFromClanBank(eq(1), eq(new BigDecimal("200")), any(java.util.function.Consumer.class));
    }

    @Test
    @Order(13)
    @DisplayName("Deve rejeitar comando bank com argumentos insuficientes")
    void testOnCommand_Bank_InsufficientArgs() {
        // Arrange
        String[] args = {"bank"};
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("Uso: /territory bank"));
    }

    @Test
    @Order(14)
    @DisplayName("Deve rejeitar comando bank deposit com quantia inválida")
    void testOnCommand_BankDeposit_InvalidAmount() {
        // Arrange
        String[] args = {"bank", "deposit", "invalid"};
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("Quantia inválida"));
    }

    @Test
    @Order(15)
    @DisplayName("Deve rejeitar comando bank deposit com quantia negativa")
    void testOnCommand_BankDeposit_NegativeAmount() {
        // Arrange
        String[] args = {"bank", "deposit", "-100"};
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("deve ser positiva"));
    }

    @Test
    @Order(16)
    @DisplayName("Deve rejeitar comando desconhecido")
    void testOnCommand_UnknownCommand() {
        // Arrange
        String[] args = {"unknown"};
        
        // Act
        boolean result = territoryCommand.onCommand(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(result).isTrue();
        verify(mockPlayer).sendMessage(contains("Subcomando não reconhecido"));
    }

    @Test
    @Order(17)
    @DisplayName("Deve completar tab para subcomandos principais")
    void testOnTabComplete_MainSubcommands() {
        // Arrange
        String[] args = {"c"};
        
        // Act
        java.util.List<String> completions = territoryCommand.onTabComplete(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(completions).contains("claim");
    }

    @Test
    @Order(18)
    @DisplayName("Deve completar tab para subcomandos de bank")
    void testOnTabComplete_BankSubcommands() {
        // Arrange
        String[] args = {"bank", "d"};
        
        // Act
        java.util.List<String> completions = territoryCommand.onTabComplete(mockPlayer, mockCommand, "territory", args);
        
        // Assert
        assertThat(completions).contains("deposit");
    }
}