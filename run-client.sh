cd src/
echo Enter the server IP
read var1
echo Enter server port
read var2
java -cp ".:../lib/*" MyClientApp $var1 $var2
