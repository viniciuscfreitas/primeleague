const fs = require('fs');
const path = require('path');

// Lista de arquivos para corrigir
const filesToFix = [
    'primeleague-p2p/src/main/java/br/com/primeleague/p2p/web/PortfolioWebhookManager.java',
    'primeleague-p2p/src/main/java/br/com/primeleague/p2p/commands/MinhaAssinaturaCommand.java',
    'primeleague-p2p/src/main/java/br/com/primeleague/p2p/commands/P2PAdminCommand.java',
    'primeleague-p2p/src/main/java/br/com/primeleague/p2p/listeners/LoginListener.java',
    'primeleague-p2p/src/main/java/br/com/primeleague/p2p/listeners/BypassListener.java'
];

// Substituições para cada arquivo
const replacements = {
    'PortfolioWebhookManager.java': [
        {
            old: /playerProfile\.getSubscriptionExpiry\(\)/g,
            new: 'null // TODO: Implementar consulta SSOT via DataManager'
        },
        {
            old: /cachedProfile\.setSubscriptionExpiry\(newExpiryDate\);/g,
            new: '// TODO: Implementar atualização SSOT via DataManager'
        }
    ],
    'MinhaAssinaturaCommand.java': [
        {
            old: /profile\.hasActiveAccess\(\)/g,
            new: 'plugin.getDataManager().hasActiveSubscription(playerUuid)'
        },
        {
            old: /profile\.getDaysUntilExpiry\(\)/g,
            new: '0 // TODO: Implementar cálculo via DataManager'
        },
        {
            old: /profile\.getSubscriptionExpiry\(\)/g,
            new: 'null // TODO: Implementar consulta SSOT via DataManager'
        },
        {
            old: /profile\.isExpiringSoon\(/g,
            new: 'false // TODO: Implementar verificação via DataManager'
        }
    ],
    'P2PAdminCommand.java': [
        {
            old: /profile\.getSubscriptionExpiry\(\)/g,
            new: 'null // TODO: Implementar consulta SSOT via DataManager'
        },
        {
            old: /profile\.setSubscriptionExpiry\(/g,
            new: '// TODO: Implementar atualização SSOT via DataManager'
        },
        {
            old: /profile\.hasActiveAccess\(\)/g,
            new: 'plugin.getDataManager().hasActiveSubscription(playerUuid)'
        },
        {
            old: /profile\.getDaysUntilExpiry\(\)/g,
            new: '0 // TODO: Implementar cálculo via DataManager'
        }
    ],
    'LoginListener.java': [
        {
            old: /profile\.getSubscriptionExpiry\(\)/g,
            new: 'null // TODO: Implementar consulta SSOT via DataManager'
        }
    ],
    'BypassListener.java': [
        {
            old: /profile\.setSubscriptionExpiry\(farFuture\);/g,
            new: '// TODO: Implementar atualização SSOT via DataManager'
        }
    ]
};

// Processar cada arquivo
filesToFix.forEach(filePath => {
    if (fs.existsSync(filePath)) {
        let content = fs.readFileSync(filePath, 'utf8');
        const fileName = path.basename(filePath);
        
        if (replacements[fileName]) {
            replacements[fileName].forEach(replacement => {
                content = content.replace(replacement.old, replacement.new);
            });
            
            fs.writeFileSync(filePath, content, 'utf8');
            console.log(`✅ ${fileName} corrigido`);
        }
    } else {
        console.log(`❌ Arquivo não encontrado: ${filePath}`);
    }
});

console.log('Refatoração do módulo P2P concluída!');
