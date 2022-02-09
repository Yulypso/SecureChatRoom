#!/usr/bin/env bash

javac --enable-preview --release 17 src/secureApp/server/*.java src/secureApp/server/Models/*.java src/secureApp/server/Utils/*.java -d prod
java --enable-preview -cp prod secureApp/server/ServerChat