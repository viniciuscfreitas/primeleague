package br.com.primeleague.core.util;

import java.nio.charset.Charset;
import java.util.UUID;

public final class UUIDUtils {

    private static final Charset UTF8 = Charset.forName("UTF-8");

    private UUIDUtils() {}

    /**
     * Gera um UUID determinístico no modo offline (pré-UUID Mojang) baseado no nome do jogador.
     * Compatível com servidores 1.5.2 em offline-mode.
     * CORRIGIDO: Usa a mesma implementação do Node.js para compatibilidade.
     */
    public static UUID offlineUUIDFromName(String playerName) {
        // Usar o algoritmo padrão do Java UUID.nameUUIDFromBytes
        String source = "OfflinePlayer:" + playerName;
        return UUID.nameUUIDFromBytes(source.getBytes(UTF8));
    }
}


