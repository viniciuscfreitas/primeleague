#!/bin/bash

echo "=== ATUALIZANDO PLUGINS PRIMELEAGUE ==="
echo

echo "[1/4] Verificando se servidor esta parado..."
if [ -f "server.jar" ]; then
    echo "ERRO: Servidor ainda esta rodando!"
    echo "Pare o servidor primeiro com: ./stop-server.sh"
    exit 1
fi

echo "[2/4] Removendo JARs antigas..."
cd plugins
rm -f primeleague-*.jar

echo "[3/4] Copiando novas JARs..."
cd ../..
cp "primeleague-core/target/primeleague-core-1.0.0.jar" "server/plugins/"
cp "primeleague-p2p/target/primeleague-p2p-1.0.0.jar" "server/plugins/"
cp "primeleague-adminshop/target/primeleague-adminshop-1.0.0.jar" "server/plugins/"
cp "primeleague-chat/target/primeleague-chat-1.0.0.jar" "server/plugins/"
cp "primeleague-clans/target/primeleague-clans-1.0.0.jar" "server/plugins/"
cp "primeleague-admin/target/primeleague-admin-1.0.0.jar" "server/plugins/"

echo "[4/4] Verificando JARs atualizadas..."
cd server/plugins
ls -la primeleague-*.jar

echo
echo "=== ATUALIZACAO CONCLUIDA! ==="
echo "Agora voce pode iniciar o servidor novamente"
