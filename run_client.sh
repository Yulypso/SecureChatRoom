#!/usr/bin/env bash

javac --enable-preview --release 17 src/Client/*.java -d prod
java --enable-preview -cp prod Client/ClientChat