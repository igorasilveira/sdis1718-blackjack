cd src/
echo Enter the server port to listen
read var1
java -cp ".:../lib/*" MyServerApp $var1
