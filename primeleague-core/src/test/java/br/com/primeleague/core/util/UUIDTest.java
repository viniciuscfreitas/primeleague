package br.com.primeleague.core.util;

import java.nio.charset.Charset;
import java.util.UUID;

public class UUIDTest {
    
    public static void main(String[] args) {
        String playerName = "mlkpiranha0";
        
        // Teste 1: Algoritmo atual do Java
        String source = "OfflinePlayer:" + playerName;
        UUID javaUuid = UUID.nameUUIDFromBytes(source.getBytes(Charset.forName("UTF-8")));
        System.out.println("Java UUID para " + playerName + ": " + javaUuid.toString());
        
        // Teste 2: Verificar se é igual ao que está no banco
        String bancoUuid = "7ef5ba1a-cd3f-3298-a25e-023619b68c0b";
        System.out.println("UUID no banco: " + bancoUuid);
        System.out.println("São iguais? " + javaUuid.toString().equals(bancoUuid));
        
        // Teste 3: Verificar o que o servidor está gerando
        String servidorUuid = "d00e7769-18de-3002-b821-cf11996f8963";
        System.out.println("UUID que o servidor está gerando: " + servidorUuid);
        System.out.println("Java vs Servidor: " + javaUuid.toString().equals(servidorUuid));
    }
}
