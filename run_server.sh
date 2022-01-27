#!/usr/bin/env bash

javac --enable-preview --release 17 src/Server/*.java src/Server/Models/*.java src/Server/Utils/*.java -d prod
java --enable-preview -cp prod Server/ServerChat