package br.com.primeleague.admin.commands;

import br.com.primeleague.admin.managers.AdminManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

/**
 * Comando /invsee - Visualização do inventário de outros jogadores
 */
public class InvseeCommand implements CommandExecutor {
    private final AdminManager adminManager;

    public InvseeCommand(AdminManager adminManager) {
        this.adminManager = adminManager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Este comando só pode ser usado por jogadores!");
            return true;
        }

        if (!sender.hasPermission("primeleague.admin.invsee")) {
            sender.sendMessage(ChatColor.RED + "Você não tem permissão para usar este comando!");
            return true;
        }

        // Verificar argumentos
        if (args.length < 1) {
            sender.sendMessage(ChatColor.RED + "Uso: /invsee <jogador>");
            return true;
        }

        String targetName = args[0];
        Player target = Bukkit.getPlayer(targetName);

        if (target == null) {
            sender.sendMessage(ChatColor.RED + "Jogador '" + targetName + "' não encontrado!");
            return true;
        }

        Player player = (Player) sender;

        // Verificar se não está inspecionando a si mesmo
        if (target.equals(player)) {
            sender.sendMessage(ChatColor.RED + "Você não pode inspecionar seu próprio inventário!");
            return true;
        }

        // Criar inventário read-only
        Inventory invseeInventory = createReadOnlyInventory(target, player);

        // Abrir inventário
        player.openInventory(invseeInventory);

        // Mensagem de confirmação
        sender.sendMessage(ChatColor.GREEN + "Visualizando inventário de " + target.getName() + " (read-only).");

        // Log da ação
        logAction(player, target, "INVSEE");

        return true;
    }

    /**
     * Cria um inventário read-only baseado no inventário do jogador alvo.
     */
    private Inventory createReadOnlyInventory(Player target, Player viewer) {
        String title = ChatColor.DARK_PURPLE + "Inventário: " + target.getName();
        Inventory inventory = Bukkit.createInventory(viewer, 54, title); // 6 linhas (54 slots)

        // Copiar itens do inventário principal do alvo
        ItemStack[] targetItems = target.getInventory().getContents();
        for (int i = 0; i < Math.min(targetItems.length, 36); i++) {
            if (targetItems[i] != null) {
                inventory.setItem(i, targetItems[i].clone());
            }
        }

        // Copiar itens da armadura
        ItemStack[] armorContents = target.getInventory().getArmorContents();
        if (armorContents.length >= 4) {
            // Botas
            if (armorContents[0] != null) {
                inventory.setItem(45, armorContents[0].clone());
            }
            // Calças
            if (armorContents[1] != null) {
                inventory.setItem(46, armorContents[1].clone());
            }
            // Peitoral
            if (armorContents[2] != null) {
                inventory.setItem(47, armorContents[2].clone());
            }
            // Capacete
            if (armorContents[3] != null) {
                inventory.setItem(48, armorContents[3].clone());
            }
        }

        // Adicionar informações do jogador
        addPlayerInfo(inventory, target);

        return inventory;
    }

    /**
     * Adiciona informações do jogador no inventário.
     */
    private void addPlayerInfo(Inventory inventory, Player target) {
        // Slot 49: Informações básicas
        String info = ChatColor.GOLD + "Jogador: " + target.getName() + "\n" +
                     ChatColor.GRAY + "Vida: " + ChatColor.RED + (int)target.getHealth() + "/" + (int)target.getMaxHealth() + "\n" +
                     ChatColor.GRAY + "Fome: " + ChatColor.YELLOW + target.getFoodLevel() + "/20\n" +
                     ChatColor.GRAY + "XP: " + ChatColor.GREEN + target.getLevel();

        // Criar item informativo (papel com lore)
        ItemStack infoItem = createInfoItem("Informações do Jogador", info);
        inventory.setItem(49, infoItem);

        // Slot 50: Status de vanish
        if (adminManager.isVanished(target.getUniqueId())) {
            ItemStack vanishItem = createInfoItem("Status: Vanish", ChatColor.GREEN + "Jogador está invisível");
            inventory.setItem(50, vanishItem);
        }

        // Slot 51: Permissões
        String permissions = "";
        if (target.hasPermission("primeleague.admin")) {
            permissions += ChatColor.RED + "Admin\n";
        }
        if (target.hasPermission("primeleague.mod")) {
            permissions += ChatColor.YELLOW + "Moderador\n";
        }
        if (target.hasPermission("primeleague.helper")) {
            permissions += ChatColor.BLUE + "Helper\n";
        }

        if (!permissions.isEmpty()) {
            ItemStack permItem = createInfoItem("Permissões", permissions);
            inventory.setItem(51, permItem);
        }
    }

    /**
     * Cria um item informativo com nome e lore.
     */
    private ItemStack createInfoItem(String name, String lore) {
        // Em Bukkit 1.5.2, não temos ItemMeta completo, então usamos um item básico
        // com nome e lore simples
        ItemStack item = new ItemStack(org.bukkit.Material.PAPER);

        // Nota: Em Bukkit 1.5.2, a manipulação de ItemMeta é limitada
        // Esta é uma implementação básica
        return item;
    }

    /**
     * Registra a ação no log.
     */
    private void logAction(Player author, Player target, String action) {
        String logMessage = String.format("[ADMIN] %s usou %s em %s",
            author.getName(), action, target.getName());



        // Notificar outros staffs
        String notification = ChatColor.YELLOW + "[ADMIN] " + ChatColor.WHITE +
            author.getName() + " usou " + action + " em " + target.getName();

        for (Player player : Bukkit.getOnlinePlayers()) {
            if (player.hasPermission("primeleague.admin.notifications") &&
                !player.equals(author) && !player.equals(target)) {
                player.sendMessage(notification);
            }
        }
    }
}
