require('dotenv').config();
const { Client, Collection, GatewayIntentBits } = require('discord.js');
const fs = require('fs');
const path = require('path');

// Importar sistema de autorização de IPs e status
const NotificationWorker = require('./workers/notification-worker');
const IPAuthHandler = require('./handlers/ip-auth-handler');
const StatusWorker = require('./workers/status-worker');

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
    console.log(`Bot iniciado como ${client.user.tag}`);

    try {
        // Registrar comandos no servidor específico
        const guild = client.guilds.cache.get(GUILD_ID);

        if (!guild) {
            console.error(`Erro: Bot não está no servidor ${GUILD_ID}`);
            return;
        }

        console.log('Registrando comandos...');

        const commands = [];
        client.commands.forEach(command => {
            console.log(`- Registrando comando: /${command.data.name}`);
            commands.push(command.data.toJSON());
        });

        await guild.commands.set(commands);
        console.log('✅ Comandos registrados com sucesso!');

        // Listar comandos disponíveis
        const registeredCommands = await guild.commands.fetch();
        console.log('\nComandos disponíveis:');
        registeredCommands.forEach(cmd => {
            console.log(`- /${cmd.name}`);
        });

        // Inicializar workers do sistema
        console.log('\n🔄 Iniciando sistema de autorização de IPs...');
        notificationWorker = new NotificationWorker(client);
        notificationWorker.start();
        console.log('✅ Sistema de autorização de IPs iniciado!');

        console.log('\n📊 Iniciando sistema de status global...');
        statusWorker = new StatusWorker(client);
        statusWorker.start();
        console.log('✅ Sistema de status global iniciado!');

    } catch (error) {
        console.error('❌ Erro ao registrar comandos:', error);
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
            // Botões de autorização de IP
            const handled = await IPAuthHandler.handleIPAuthInteraction(interaction);
            if (!handled) {
                console.log(`[Bot] Interação de botão não tratada: ${interaction.customId}`);
            }
        }
    } catch (error) {
        console.error('[Bot] Erro ao processar interação:', error);
        
        const errorMessage = {
            content: '❌ **Erro interno:** Ocorreu um erro ao processar esta ação.',
            ephemeral: true
        };

        if (interaction.replied || interaction.deferred) {
            await interaction.editReply(errorMessage);
        } else {
            await interaction.reply(errorMessage);
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

// Login
client.login(TOKEN);
