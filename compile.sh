./reset-files.sh
./generate-keys.sh
./database.sh
cd src/
javac -cp ".:../lib/*" *.java
chmod +x ../*.sh
