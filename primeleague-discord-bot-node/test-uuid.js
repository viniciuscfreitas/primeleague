const crypto = require('crypto');

function generateUUID(name) {
    // Java UUID.nameUUIDFromBytes faz MD5 do source string diretamente
    const source = "OfflinePlayer:" + name;
    const hash = crypto.createHash('md5').update(source).digest();
    
    // Java UUID.nameUUIDFromBytes usa formato específico
    // Primeiros 4 bytes: little-endian
    // Próximos 2 bytes: little-endian  
    // Próximos 2 bytes: little-endian
    // Próximos 2 bytes: big-endian
    // Próximos 2 bytes: big-endian
    // Últimos 4 bytes: big-endian
    
    // Converter diretamente do hash MD5 para UUID
    const uuid = [
        hash.toString('hex').substring(0, 8),
        hash.toString('hex').substring(8, 12),
        hash.toString('hex').substring(12, 16),
        hash.toString('hex').substring(16, 20),
        hash.toString('hex').substring(20, 32)
    ].join('-');
    
    return uuid;
}

// Teste
const testName = "mlkpiranha0";
const source = "OfflinePlayer:" + testName;
const hash = crypto.createHash('md5').update(source).digest();

console.log(`Nome: ${testName}`);
console.log(`Source: ${source}`);
console.log(`MD5 Hash (hex): ${hash.toString('hex')}`);
console.log(`MD5 Hash (bytes): [${Array.from(hash).map(b => b.toString(16).padStart(2, '0')).join(', ')}]`);

const generatedUUID = generateUUID(testName);
const expectedUUID = "d00e7769-18de-3002-b821-cf11996f8963";

console.log(`UUID gerado: ${generatedUUID}`);
console.log(`UUID esperado: ${expectedUUID}`);
console.log(`São iguais? ${generatedUUID === expectedUUID}`);
