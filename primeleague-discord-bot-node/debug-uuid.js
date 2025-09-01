const crypto = require('crypto');

// Função para gerar UUID offline (mesmo algoritmo do Java)
function generateOfflineUUID(username) {
    const source = `OfflinePlayer:${username}`;
    const hash = crypto.createHash('md5').update(source, 'utf8').digest();
    
    // Converter para UUID usando o mesmo algoritmo do Java
    let msb = 0;
    let lsb = 0;
    
    for (let i = 0; i < 8; i++) {
        msb = (msb << 8) | (hash[i] & 0xff);
    }
    for (let i = 8; i < 16; i++) {
        lsb = (lsb << 8) | (hash[i] & 0xff);
    }
    
    // Aplicar versão e variante
    msb &= ~(0xf << 12);
    msb |= 3 << 12; // versão 3
    lsb &= ~(0x3 << 62);
    lsb |= 2 << 62; // variante
    
    return `${msb.toString(16).padStart(16, '0')}-${lsb.toString(16).padStart(16, '0').replace(/(.{4})/g, '$1-').slice(0, -1)}`;
}

// Função para formatar UUID no padrão correto
function formatUUID(uuid) {
    return uuid.replace(/(.{8})(.{4})(.{4})(.{4})(.{12})/, '$1-$2-$3-$4-$5');
}

// Testar com diferentes nomes
const testNames = ['vini', 'vinicff', 'mlkpiranha0', 'testplayer'];

console.log('🔍 === TESTE DE GERAÇÃO DE UUID ===\n');

testNames.forEach(name => {
    const uuid = generateOfflineUUID(name);
    console.log(`👤 Nome: ${name}`);
    console.log(`   Source: OfflinePlayer:${name}`);
    console.log(`   UUID: ${uuid}`);
    console.log('');
});

// Testar com nomes conhecidos do banco
console.log('🔍 === COMPARAÇÃO COM UUIDs CONHECIDOS ===\n');

const knownUUIDs = {
    'vini': 'b2d67524-ac9a-31a0-80c7-7acd45619820',
    'vinicff': '3e556f49-c226-3253-8408-9824b21a6d6a'
};

Object.entries(knownUUIDs).forEach(([name, expectedUUID]) => {
    const generatedUUID = generateOfflineUUID(name);
    const match = generatedUUID === expectedUUID;
    
    console.log(`👤 ${name}:`);
    console.log(`   Esperado: ${expectedUUID}`);
    console.log(`   Gerado:   ${generatedUUID}`);
    console.log(`   Match:    ${match ? '✅' : '❌'}`);
    console.log('');
});

// Testar com diferentes algoritmos
console.log('🔍 === TESTE DE ALGORITMOS ALTERNATIVOS ===\n');

function generateUUIDv3(name) {
    const source = `OfflinePlayer:${name}`;
    const hash = crypto.createHash('md5').update(source, 'utf8').digest();
    
    // Algoritmo UUID v3 padrão
    const uuid = formatUUID(hash.toString('hex'));
    return uuid;
}

function generateUUIDv5(name) {
    const source = `OfflinePlayer:${name}`;
    const hash = crypto.createHash('sha1').update(source, 'utf8').digest();
    
    // Algoritmo UUID v5
    const uuid = formatUUID(hash.toString('hex'));
    return uuid;
}

testNames.forEach(name => {
    console.log(`👤 ${name}:`);
    console.log(`   Offline: ${generateOfflineUUID(name)}`);
    console.log(`   UUID v3: ${generateUUIDv3(name)}`);
    console.log(`   UUID v5: ${generateUUIDv5(name)}`);
    console.log('');
});

// Testar com nomes especiais
console.log('🔍 === TESTE COM NOMES ESPECIAIS ===\n');

const specialNames = ['Test123', 'player_name', 'PLAYER', 'Player'];

specialNames.forEach(name => {
    const uuid = generateOfflineUUID(name);
    console.log(`👤 "${name}": ${uuid}`);
});

console.log('✅ === TESTE DE UUID CONCLUÍDO ===');
