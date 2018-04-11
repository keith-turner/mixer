#!/bin/bash

case "$1" in
  "mini")
    mvn  clean compile exec:java  -Dexec.mainClass=mixer.MiniMixer -Dexec.args="${*:2}"
    ;;
  "shell")
    mvn -q compile exec:java  -Dexec.mainClass=mixer.Shell -Dexec.args="${*:2}"
    ;;
  "graphviz")
    mvn -q compile exec:java  -Dexec.mainClass=mixer.Graphviz -Dexec.args="${*:2}"
    ;;
esac
