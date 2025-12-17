#!/bin/bash

echo "[SYSTEM]"
echo "Kernel:      $(uname -sr)"
echo "OS:          $(uname -om)"
echo "DATE:        $(date)"
echo "UPTIME:      $(uptime -p)"
echo "FREE MEMORY: $(awk '/MemFree/ { printf "%.3f KB\n", $2/1024 }' /proc/meminfo)"
echo
echo "[RUNTIME]"
echo "User:        $(whoami)"
echo "CWD:         $(pwd)"

if [[ -f "/var/lib/postgresql/proof.txt" ]]; then
  echo
  echo "[FLAG]"
  /usr/bin/cat /root/flag.txt
fi
