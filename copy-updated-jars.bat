@echo off
echo Copiando JARs atualizados para o servidor...

copy primeleague-api\target\primeleague-api-1.0.0.jar server\plugins\
copy primeleague-core\target\primeleague-core-1.0.0.jar server\plugins\
copy primeleague-p2p\target\primeleague-p2p-1.0.0.jar server\plugins\
copy primeleague-clans\target\primeleague-clans-1.0.0.jar server\plugins\
copy primeleague-admin\target\primeleague-admin-1.0.0.jar server\plugins\
copy primeleague-adminshop\target\primeleague-adminshop-1.0.0.jar server\plugins\
copy primeleague-chat\target\primeleague-chat-1.0.0.jar server\plugins\

echo JARs copiados com sucesso!
pause
