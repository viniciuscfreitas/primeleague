const { SlashCommandBuilder, EmbedBuilder } = require('discord.js');
const {
    getDiscordLinksById,
    removeClanMember,
    getClanActiveSessionsCount,
    createServerNotification
} = require('../database/mysql');

module.exports = {
    data: new SlashCommandBuilder()
        .setName('remover-membro')
        .setDescription('Remove uma conta do seu clã')
        .addStringOption(option =>
            option.setName('nickname')
                .setDescription('Nickname da conta a ser removida do clã')
                .setRequired(true)),

    async execute(interaction) {
        await interaction.deferReply({ ephemeral: true });

        try {
            const nickname = interaction.options.getString('nickname');
            const discordId = interaction.user.id;

            // 1. Buscar membros do clã
            const clanMembers = await getDiscordLinksById(discordId);
            
            if (!clanMembers || clanMembers.length === 0) {
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('❌ Clã Não Encontrado')
                    .setDescription(
                        'Você não possui um clã registrado.\n\n' +
                        'Use `/registrar <nickname>` para criar seu clã.'
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 2. Verificar se a conta existe no clã
            const memberToRemove = clanMembers.find(member => 
                member.player_name.toLowerCase() === nickname.toLowerCase()
            );

            if (!memberToRemove) {
                const membersList = clanMembers.map(m => `• ${m.player_name}`).join('\n');
                
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('❌ Conta Não Encontrada')
                    .setDescription(
                        `A conta \`${nickname}\` não foi encontrada no seu clã.\n\n` +
                        '**Contas vinculadas ao seu clã:**\n' +
                        membersList
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 3. Verificar se é a conta principal
            if (memberToRemove.is_primary) {
                const otherMembers = clanMembers.filter(m => !m.is_primary);
                
                if (otherMembers.length > 0) {
                    const embed = new EmbedBuilder()
                        .setColor('#FFB84D')
                        .setTitle('⚠️ Não é Possível Remover')
                        .setDescription(
                            `**${memberToRemove.player_name}** é a conta principal do clã.\n\n` +
                            '**Para remover a conta principal:**\n' +
                            '1. Primeiro remova todos os outros membros\n' +
                            '2. Ou transfira a liderança para outro membro\n\n' +
                            `**Outros membros (${otherMembers.length}):**\n` +
                            otherMembers.map(m => `• ${m.player_name}`).join('\n')
                        )
                        .setTimestamp();

                    return interaction.editReply({ embeds: [embed] });
                }
            }

            // 4. Verificar se o jogador está online
            const activeSessions = await getClanActiveSessionsCount(discordId);
            // Aqui você verificaria se este jogador específico está online
            // Por simplicidade, vamos assumir que não está
            
            // 5. Remover membro do clã
            const success = await removeClanMember(memberToRemove.player_uuid);
            
            if (!success) {
                const embed = new EmbedBuilder()
                    .setColor('#FF6B6B')
                    .setTitle('❌ Erro na Remoção')
                    .setDescription(
                        'Falha ao remover o membro do clã. Tente novamente.\n\n' +
                        'Se o problema persistir, entre em contato com a administração.'
                    )
                    .setTimestamp();

                return interaction.editReply({ embeds: [embed] });
            }

            // 6. Criar notificação para o servidor (para desconectar se estiver online)
            await createServerNotification('CLAN_MEMBER_REMOVED', memberToRemove.player_name, {
                discord_id: discordId,
                removed_by: interaction.user.tag,
                player_uuid: memberToRemove.player_uuid,
                reason: 'Removido pelo líder do clã'
            });

            // 7. Resposta de sucesso
            const remainingMembers = clanMembers.length - 1;
            const embed = new EmbedBuilder()
                .setColor('#4ECDC4')
                .setTitle('✅ Membro Removido')
                .setDescription(
                    `**${memberToRemove.player_name}** foi removido do seu clã com sucesso.\n\n` +
                    '**Detalhes da Remoção:**\n' +
                    `• Conta: \`${memberToRemove.player_name}\`\n` +
                    `• Tipo: ${memberToRemove.is_primary ? 'Conta Principal' : 'Membro'}\n` +
                    `• Removido por: ${interaction.user}\n` +
                    `• Data: <t:${Math.floor(Date.now() / 1000)}:F>\n\n` +
                    `**Status do Clã:**\n` +
                    `• Membros restantes: **${remainingMembers}**\n` +
                    `• A conta foi desconectada automaticamente se estava online`
                )
                .setTimestamp();

            // 8. Adicionar informações adicionais baseadas no contexto
            if (remainingMembers === 0) {
                embed.addFields({
                    name: '⚠️ Clã Vazio',
                    value: 
                        'Seu clã não possui mais membros vinculados.\n\n' +
                        '**Próximos Passos:**\n' +
                        '• Use `/registrar <nickname>` para adicionar uma nova conta principal\n' +
                        '• Ou mantenha o clã vazio até decidir adicionar novos membros',
                    inline: false
                });
            } else if (memberToRemove.is_primary && remainingMembers === 1) {
                const newPrimary = clanMembers.find(m => !m.is_primary);
                embed.addFields({
                    name: '👑 Nova Conta Principal',
                    value: 
                        `**${newPrimary.player_name}** agora é a conta principal do clã.\n\n` +
                        'A liderança foi transferida automaticamente.',
                    inline: false
                });
            }

            // 9. Atualizar estatísticas
            embed.setFooter({ 
                text: `Clã agora possui ${remainingMembers} membro(s) • Remoção realizada com sucesso` 
            });

            await interaction.editReply({ embeds: [embed] });

        } catch (error) {
            console.error('Erro no comando /remover-membro:', error);
            
            const errorEmbed = new EmbedBuilder()
                .setColor('#FF6B6B')
                .setTitle('❌ Erro Interno')
                .setDescription(
                    'Ocorreu um erro ao remover o membro do clã.\n\n' +
                    'Tente novamente em instantes ou entre em contato com a administração.'
                )
                .setTimestamp();

            return interaction.editReply({ embeds: [errorEmbed] });
        }
    },
};
