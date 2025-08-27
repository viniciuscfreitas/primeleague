const { SlashCommandBuilder } = require('discord.js');
const { getPlayerAccountInfo, getVerificationStatus } = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('minha-conta')
        .setDescription('Mostra informações da sua conta'),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const discordId = interaction.user.id;
            const accountInfo = await getPlayerAccountInfo(discordId);

            if (!accountInfo) {
                // Verificar se há registro pendente de verificação
                const verificationStatus = await getVerificationStatus(discordId);

                if (verificationStatus && !verificationStatus.verified) {
                    return interaction.editReply({
                        content: '⏳ **Verificação Pendente**\n\n' +
                                'Seu registro está aguardando verificação!\n\n' +
                                '**🎮 PRÓXIMO PASSO:**\n' +
                                'Digite `/verify [codigo]` **no servidor Minecraft** para confirmar seu registro.\n\n' +
                                '**⚠️ Código expirado?**\n' +
                                'Use `/registrar <nickname>` novamente para gerar um novo código.'
                    });
                } else {
                    return interaction.editReply({
                        content: '❌ **Conta não registrada!**\n\n' +
                                'Você ainda não registrou sua conta Discord com um nickname do Minecraft.\n\n' +
                                '**Como registrar:**\n' +
                                '• Use `/registrar <nickname>` para vincular sua conta\n' +
                                '• Complete a verificação no servidor Minecraft\n' +
                                '• O nickname deve existir no servidor'
                    });
                }
            }

            // Construir resposta
            let response = '🎮 **Informações da Sua Conta**\n\n';

            // Informações básicas
            response += `**Nickname:** \`${accountInfo.player_name}\`\n`;
            response += `**Discord:** ${interaction.user}\n`;
            response += `**UUID:** \`${accountInfo.player_uuid}\`\n\n`;

            // Estatísticas
            response += '📊 **Estatísticas**\n';
            response += `**ELO:** ${accountInfo.elo || 0}\n`;
            response += `**Dinheiro:** $${(parseFloat(accountInfo.money) || 0).toFixed(2)}\n\n`;

            // Status da assinatura
            response += '💎 **Status da Assinatura**\n';
            if (!accountInfo.subscription_expires_at) {
                response += '**Status:** ❌ Sem assinatura\n';
                response += '**Expira:** Nunca\n';
            } else {
                const expiryDate = new Date(accountInfo.subscription_expires_at);
                const now = new Date();

                if (expiryDate > now) {
                    const daysLeft = Math.ceil((expiryDate - now) / (1000 * 60 * 60 * 24));
                    response += '**Status:** ✅ Ativa\n';
                    response += `**Expira:** ${expiryDate.toLocaleString('pt-BR')}\n`;
                    response += `**Dias restantes:** ${daysLeft} dias\n`;
                } else {
                    response += '**Status:** ❌ Expirada\n';
                    response += `**Expirou:** ${expiryDate.toLocaleString('pt-BR')}\n`;
                }
            }

            response += '\n**Comandos disponíveis:**\n';
            response += '• `/renovar` - Gerar link de renovação\n';
            response += '• `/minha-conta` - Ver estas informações novamente';

            return interaction.editReply({ content: response });

        } catch (error) {
            console.error('Erro no comando /minha-conta:', error);
            return interaction.editReply({
                content: '❌ **Erro interno:** Ocorreu um erro inesperado. Tente novamente.'
            });
        }
    },
};
