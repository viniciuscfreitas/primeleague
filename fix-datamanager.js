const fs = require('fs');
const path = require('path');

const filePath = 'primeleague-core/src/main/java/br/com/primeleague/core/managers/DataManager.java';

// Ler o arquivo
let content = fs.readFileSync(filePath, 'utf8');

// Lista de linhas para remover
const linesToRemove = [
    'profile.setSubscriptionExpiry(null); // Será carregado via Discord ID',
    'profile.setDonorTier(0); // Será carregado via Discord ID',
    'profile.setDonorTierExpiresAt(null); // Será carregado via Discord ID'
];

// Remover cada linha
linesToRemove.forEach(line => {
    const regex = new RegExp(`\\s*${line.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')}\\s*\\n?`, 'g');
    content = content.replace(regex, '');
});

// Salvar o arquivo
fs.writeFileSync(filePath, content, 'utf8');

console.log('DataManager.java atualizado - linhas problemáticas removidas');
