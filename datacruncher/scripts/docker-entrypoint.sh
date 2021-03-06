#!/bin/bash

# container entrypoint that periodically runs the
# dc update cycle when the server resources are 
# idle to avoid disrupting other projects on dev machine.

# Max total cpu usage threshold for a cycle to begin (percent, 0-100)
cpu_threshold='20'
# Minimum free memory threshold for a cycle to begin (MB)
mem_threshold='8000'
# max 15m rounded CPU load avg
load_avg_threshold="1"

cpu_load_avg_low () {
  # 15 min
  F15M=`(uptime | awk -F "load average:" '{ print $2 }' | cut -d, -f3) | sed 's/ //g'`
  echo "[+] Current 15 minute load average: ${F15M} (max ${load_avg_threshold})"
  # comparison in bash needs int
  l_avg_int=`printf "%.0f\n" "$F15M"`
  
  if [[ $load_avg_threshold -gt $l_avg_int ]]; then
    return 0 # true
  else
    return 1
  fi
}

cpu_is_idle () {
  # percent
  cpu_idle=`top -b -n 1 | grep Cpu | awk '{print $8}'|cut -f 1 -d "."`
  cpu_use=`expr 100 - $cpu_idle`
  echo "[+] Current cpu utilization: $cpu_use % (max ${cpu_threshold} %)"
  if [ $cpu_use -lt $cpu_threshold ]; then
    return 0 # true
  else
    return 1
  fi
}

mem_is_idle () {
  # MB units
  mem_free=`free -m | grep "Mem" | awk '{print $4+$6}'`
  echo "[+] Current free memory: $mem_free MB (min ${mem_threshold})"
  if [ $mem_free -ge $mem_threshold  ]; then
    return 0 # true
  else
    return 1 # false
  fi
}

server_is_idle () {
  if cpu_load_avg_low && cpu_is_idle && mem_is_idle ; then
    echo "[+] Server is idle."
    return 0 # true
  else
    echo "[-] Server not idle."
    return 1
  fi
}


while true; do
  until server_is_idle; do
    echo "[`date`] Waiting for server resources to free up before running dc."
    sleep 3m
  done

  echo "[`date`] Running dc."
  /opt/docker/bin/datacruncher cycle

  if [[ $? -eq 0 ]]; then 
    echo "[`date`] dc execution finished. Sleeping until next cycle window."
    sleep 8h
  fi
done
