#!/bin/sh

## first bring down ethernet interface and kill dhcpcd
ifconfig eth0 down
## code taken from Puppy Network Wizard
if [ -d /var/lib/dhcpcd ] ; then
  if [ -s /var/run/dhcpcd-eth0.pid ] ; then
    kill $( cat /var/run/dhcpcd-eth0.pid )
    rm -f /var/run/dhcpcd-eth0.* 2>/dev/null
fi
  #begin rerwin - Retain duid, if any, so all interfaces can use
  #it (per ipv6) or delete it if using MAC address as client ID.    rerwin
  rm -f /var/lib/dhcpcd/dhcpcd-eth0.* 2>/dev/null  #.info
#end rerwin
  #rm -f /var/run/dhcpcd-eth0.* 2>/dev/null #.pid
elif [ -d /etc/dhcpc ];then
  if [ -s /etc/dhcpc/dhcpcd-eth0.pid ] ; then
    kill $( cat /etc/dhcpc/dhcpcd-eth0.pid )
    rm /etc/dhcpc/dhcpcd-eth0.pid 2>/dev/null
  fi
  rm /etc/dhcpc/dhcpcd-eth0.* 2>/dev/null 
  #if left over from last session, causes trouble.	  
fi

## put wifi interface into Access Point mode
ifconfig ath0 down
wlanconfig ath0 destroy
wlanconfig ath0 create wlandev wifi0 wlanmode ap
sleep 1

## for hostap_pci/hostap_cs comment out the previous 3 lines, an uncomment the following 2 lines
#ifconfig wlan0 down
#iwconfig wlan0 mode Master

## bridge the ethernet interface to the wifi interface
ifconfig eth0 0.0.0.0 up
ifconfig ath0 0.0.0.0 up
modprobe bridge
sleep 1
brctl addbr br0
brctl addif br0 eth0
brctl addif br0 ath0

## acquire an IP address on the br0 (bridged) interface
rm -f /var/run/dhcpcd-br0.*
dhcpcd -t 30 -h puppypc -d br0

## finally run the hostapd daemon
hostapd /etc/hostapd.conf
