cd keys/
echo ENTER SERVER IP FOR CERTIFICATE
read ip
mkdir server
cd server
keytool -genkeypair -alias server \
    -keyalg RSA -validity 7 -keystore keystoreServer \
    -ext san=ip:$ip \
    -dname "CN=127.0.0.1, OU=sdis-blackjack, O=FEUP, L=Porto, S=Porto, C=PT" \
    -storepass sdisServer -keypass sdisServer
cd ..
mkdir client
cd client
keytool -genkeypair -alias client \
    -keyalg RSA -validity 7 -keystore keystoreClient \
    -dname "CN=127.0.0.1, OU=sdis-blackjack, O=FEUP, L=Porto, S=Porto, C=PT" \
    -storepass sdisClient -keypass sdisClient
cd ..
echo server
cd server
keytool -export -alias server -keystore keystoreServer -rfc -file server.cer -storepass sdisServer
cd ..
cd client
echo client
keytool -export -alias client -keystore keystoreClient -rfc -file client.cer -storepass sdisClient
cd ..
keytool -import -alias servercert -file server/server.cer -keystore truststore -storepass truststore -noprompt

keytool -import -alias clientcert -file client/client.cer -keystore truststore -storepass truststore -noprompt

keytool -list -v -keystore truststore -storepass truststore


