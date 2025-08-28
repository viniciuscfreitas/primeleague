require('dotenv').config();
const { Client, Collection, GatewayIntentBits } = require('discord.js');
const fs = require('fs');
const path = require('path');

// Importar sistema de autorização de IPs e status
const NotificationWorker = require('./workers/notification-worker');
const IPAuthHandler = require('./handlers/ip-auth-handler');
const StatusWorker = require('./workers/status-worker');
const SubscriptionButtonHandler = require('./handlers/subscription-button-handler');
const DonorButtonHandler = require('./handlers/donor-button-handler');

// Criar cliente Discord
const client = new Client({
    intents: [
        GatewayIntentBits.Guilds,
        GatewayIntentBits.GuildMessages,
        GatewayIntentBits.MessageContent
    ]
});

// Coleção de comandos
client.commands = new Collection();

// Workers do sistema
let notificationWorker;
let statusWorker;

// Carregar comandos
const commandsPath = path.join(__dirname, 'commands');
const commandFiles = fs.readdirSync(commandsPath).filter(file => file.endsWith('.js'));

for (const file of commandFiles) {
    const filePath = path.join(commandsPath, file);
    const command = require(filePath);

    if ('data' in command && 'execute' in command) {
        client.commands.set(command.data.name, command);
    } else {
        console.log(`[AVISO] O comando em ${filePath} está faltando propriedades obrigatórias.`);
    }
}

// Evento ready
client.once('ready', async () => {
    console.log(`§9[Bot] Bot iniciado como ${client.user.tag}`);

    try {
        // Registrar comandos no servidor específico
        const guild = client.guilds.cache.get(GUILD_ID);

        if (!guild) {
            console.error(`§c[Bot] Erro: Bot não está no servidor ${GUILD_ID}`);
            return;
        }

        const commands = [];
        client.commands.forEach(command => {
            commands.push(command.data.toJSON());
        });

        await guild.commands.set(commands);

        // Inicializar workers do sistema
        notificationWorker = new NotificationWorker(client);
        notificationWorker.start();

        statusWorker = new StatusWorker(client);
        statusWorker.start();

    } catch (error) {
        console.error('§c[Bot] Erro ao registrar comandos:', error);
    }
});

// Evento interactionCreate
client.on('interactionCreate', async interaction => {
    try {
        if (interaction.isChatInputCommand()) {
            // Comandos slash
            const command = client.commands.get(interaction.commandName);
            if (!command) return;

            await command.execute(interaction);
            
        } else if (interaction.isButton()) {
            const { customId } = interaction;
            console.log(`[Bot] Processando botão: ${customId}`);
            
            let handled = false;
            
            // Verificar se é um botão de upgrade de doador (prioridade alta)
            if (customId.startsWith('upgrade_to_') || customId.startsWith('donor_')) {
                console.log(`[Bot] Tentando DonorButtonHandler para: ${customId}`);
                handled = await DonorButtonHandler.handleDonorButton(interaction);
            }
            
            // Se não foi tratado, tentar IPAuthHandler
            if (!handled) {
                console.log(`[Bot] Tentando IPAuthHandler para: ${customId}`);
                handled = await IPAuthHandler.handleIPAuthInteraction(interaction);
            }
            
            // Se não foi tratado, tentar SubscriptionButtonHandler
            if (!handled) {
                console.log(`[Bot] Tentando SubscriptionButtonHandler para: ${customId}`);
                handled = await SubscriptionButtonHandler.handleSubscriptionButton(interaction);
            }
            
            if (!handled) {
                console.log(`[Bot] Interação de botão não tratada: ${customId}`);
            }
        }
    } catch (error) {
        console.error('[Bot] Erro ao processar interação:', error);
        
        try {
            const errorMessage = {
                content: '❌ **Erro interno:** Ocorreu um erro ao processar esta ação. Tente novamente.',
                ephemeral: true
            };

            if (interaction.replied || interaction.deferred) {
                await interaction.editReply(errorMessage);
            } else {
                await interaction.reply(errorMessage);
            }
        } catch (replyError) {
            console.error('[Bot] Erro ao enviar mensagem de erro:', replyError);
        }
    }
});

// Evento de desligamento - parar workers
process.on('SIGINT', () => {
    console.log('\n🔄 Encerrando bot...');
    
    if (notificationWorker) {
        notificationWorker.stop();
    }
    
    if (statusWorker) {
        statusWorker.stop();
    }
    
    console.log('✅ Bot encerrado graciosamente');
    process.exit(0);
});

// Token do bot (deve ser configurado via variável de ambiente)
const TOKEN = process.env.DISCORD_BOT_TOKEN || 'SEU_TOKEN_AQUI';
const GUILD_ID = process.env.DISCORD_GUILD_ID || '1344225249826443266';

// Configurar variáveis de ambiente para workers
process.env.STATUS_CHANNEL_ID = process.env.STATUS_CHANNEL_ID || '1405718108768829592';

// Login
client.login(TOKEN);
