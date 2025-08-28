const crypto = require('crypto');

/**
 * Gera um UUID v3 (name-based, MD5) compatível com o método UUID.nameUUIDFromBytes() do Java.
 * Isso segue o padrão RFC 4122.
 * @param {string} name O nome a partir do qual o UUID será gerado (ex: "vini").
 * @returns {string} O UUID v3 formatado.
 */
function generateJavaCompatibleUUID(name) {
    // 1. Prefixo usado pelo servidor Java (OfflinePlayer)
    const source = "OfflinePlayer:" + name;

    // 2. Gera o hash MD5 e obtém os 16 bytes em um Buffer
    const hash = crypto.createHash('md5').update(source, 'utf-8').digest();

    // 3. A MÁGICA DA RFC 4122: Ajusta os bits de versão e variante
    //    Isso é o que o UUID.nameUUIDFromBytes() do Java faz internamente.

    // No 7º byte (índice 6), define a versão para 3.
    // Limpa os 4 bits mais significativos (AND com 00001111)
    // e define como 0011 (OR com 00110000)
    hash[6] = (hash[6] & 0x0f) | 0x30;

    // No 9º byte (índice 8), define a variante para RFC 4122.
    // Limpa os 2 bits mais significativos (AND com 00111111)
    // e define como 10 (OR com 10000000)
    hash[8] = (hash[8] & 0x3f) | 0x80;

    // 4. Converte o Buffer modificado para a string de UUID no formato 8-4-4-4-12
    const uuid = [
        hash.toString('hex', 0, 4),
        hash.toString('hex', 4, 6),
        hash.toString('hex', 6, 8),
        hash.toString('hex', 8, 10),
        hash.toString('hex', 10, 16)
    ].join('-');

    return uuid;
}

// --- Teste com o player "vini" ---
const playerName = "vini";
const generatedUUID = generateJavaCompatibleUUID(playerName);
const expectedUUID = "b2d67524-ac9a-31a0-80c7-7acd45619820";

console.log(`=== TESTE DE UUID COMPATÍVEL COM JAVA ===`);
console.log(`Player: ${playerName}`);
console.log(`Java gerou:      ${expectedUUID}`);
console.log(`Node.js corrigido: ${generatedUUID}`);
console.log(`Match:             ${generatedUUID === expectedUUID ? '✅ CORRETO!' : '❌ INCORRETO!'}`);

if (generatedUUID === expectedUUID) {
    console.log(`\n🎉 SUCESSO! O algoritmo está corrigido e compatível com Java!`);
} else {
    console.log(`\n❌ Ainda há diferenças. Verificar algoritmo.`);
}
