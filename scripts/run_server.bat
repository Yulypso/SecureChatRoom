javac --enable-preview --release 17 src/secureApp/server/*.java src/secureApp/server/Models/*.java src/secureApp/server/Utils/*.java -d out
java --enable-preview -cp out secureApp/server/ServerChat