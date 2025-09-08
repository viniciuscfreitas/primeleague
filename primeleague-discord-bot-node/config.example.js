// ========================================
// CONFIGURAÇÕES DO BOT DISCORD - EXEMPLO
// ========================================
// Copie este arquivo para .env e configure as variáveis

module.exports = {
    // ========================================
    // CONFIGURAÇÕES DO BOT DISCORD
    // ========================================
    
    // Token do bot Discord (obrigatório)
    DISCORD_BOT_TOKEN: 'seu_token_aqui',
    
    // ID do servidor Discord (obrigatório)
    DISCORD_GUILD_ID: 'seu_guild_id_aqui',
    
    // ID do canal de status (opcional)
    STATUS_CHANNEL_ID: 'seu_status_channel_id_aqui',
    
    // Porta do webhook (opcional, padrão: 3000)
    WEBHOOK_PORT: 3000,
    
    // ========================================
    // CONFIGURAÇÕES DO BANCO DE DADOS
    // ========================================
    
    // Configurações do MySQL
    DB_HOST: 'localhost',
    DB_PORT: 3306,
    DB_USER: 'seu_usuario',
    DB_PASSWORD: 'sua_senha',
    DB_NAME: 'primeleague',
    
    // ========================================
    // CONFIGURAÇÕES ADICIONAIS
    // ========================================
    
    // URL base da API (opcional)
    API_BASE_URL: 'http://localhost:8080',
    
    // Chave secreta para webhooks (opcional)
    WEBHOOK_SECRET: 'sua_chave_secreta_aqui'
};
