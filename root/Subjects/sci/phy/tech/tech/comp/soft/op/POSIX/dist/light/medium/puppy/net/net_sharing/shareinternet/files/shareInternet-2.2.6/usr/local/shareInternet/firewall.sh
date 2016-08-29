#!/bin/sh

APPDIR="$0"
APPDIR="${APPDIR%\/*}"

[ "$1" == "start" ] && modprobe ipt_MASQUERADE

#
# Stopping forwarding (this script may be run during normal uptime because
# for re-lease of DHCP or demand dialing / PPPoE.
#
echo "0" > /proc/sys/net/ipv4/ip_forward
[ "$1" == "stop" ] && exit 0 #stop data flowing

#
# Firewall setup.
#
[ -f /root/.shareInternet/firewall.conf ] || exit 1
. /root/.shareInternet/firewall.conf

#
# Flushing the chains.
#

iptables -F
iptables -X
iptables -Z
for i in `cat /proc/net/ip_tables_names`; do 
    iptables -F -t $i 
    iptables -X -t $i 
    iptables -Z -t $i 
done

#
# Policy for chains DROP everything
#

iptables -P INPUT DROP
iptables -P OUTPUT DROP
iptables -P FORWARD DROP

#
# SYN-Flooding protection
# Looks good and nicked from a firewall script mentioned on floppyfw.something.
# Didn't work that well.. 
#
iptables -N syn-flood
iptables -A INPUT -i ${OUTSIDE_DEVICE} -p tcp --syn -j syn-flood
iptables -A syn-flood -m limit --limit 1/s --limit-burst 4 -j RETURN
iptables -A syn-flood -j DROP
# Make sure NEW tcp connections are SYN packets
iptables -A INPUT -i ${OUTSIDE_DEVICE} -p tcp ! --syn -m state --state NEW -j DROP 

#
# Good old masquerading.
#
for INIP in $INSIDE_IPS; do
    iptables -t nat -A POSTROUTING -s ${INIP%.*}.0/24 -o ${OUTSIDE_DEVICE} -j MASQUERADE
#    iptables -t nat -A POSTROUTING -s ${INIP%.*}.0/24 -o ${OUTSIDE_DEVICE} -j SNAT --to-source ${OUTSIDE_IP}
done
 
#
# Keep state and open up for outgoing connections.
#
iptables -A FORWARD -m state --state ESTABLISHED,RELATED -j ACCEPT
for IDEV in $INSIDE_DEVICES; do
    iptables -A FORWARD -m state --state NEW -i ${IDEV} -j ACCEPT
done
iptables -A FORWARD -m state --state NEW,INVALID -i ${OUTSIDE_DEVICE} -j DROP

#
# We don't like the NetBIOS and Samba leaking..
#
#iptables -t nat -A PREROUTING -p TCP --dport 135:139 -j DROP
#iptables -t nat -A PREROUTING -p UDP --dport 135:139 -j DROP
#iptables -t nat -A PREROUTING -p TCP --dport 445 -j DROP
#iptables -t nat -A PREROUTING -p UDP --dport 445 -j DROP

#
# We would like to ask for names from our floppyfw box
#
iptables -A INPUT -m state --state ESTABLISHED,RELATED -j ACCEPT
iptables -A OUTPUT -m state --state NEW,ESTABLISHED,RELATED -j ACCEPT

# Ping and friends.
iptables -A OUTPUT -p icmp -j ACCEPT # to both sides.
iptables -A INPUT  -p icmp -j ACCEPT 

# And also, DHCP, but we can basically accept anything from the inside.
for IDEV in $INSIDE_DEVICES; do
    iptables -A INPUT -i ${IDEV} -j ACCEPT
    iptables -A OUTPUT -o ${IDEV} -j ACCEPT
done
# And also accept talking to ourself.
iptables -A INPUT -i lo -j ACCEPT
iptables -A OUTPUT -o lo -j ACCEPT

#
# reject ident
#iptables -A INPUT -p TCP --dport 113 -i ${OUTSIDE_DEVICE} -j REJECT --reject-with tcp-reset

# 
# Running extra scripts.
#
for i in /root/.shareInternet/iptablesExtraRules/*; do
    if [ -x $i ]; then
        $i
    fi
done

# 
#  The insert stuff into the kernel (ipsysctl) - section:
#
# Some of there goes under the "Better safe than sorry" - banner.
#

#
# This enables dynamic IP address following
#
echo 7 > /proc/sys/net/ipv4/ip_dynaddr

#
# trying to stop some smurf attacks.
#
echo 1 > /proc/sys/net/ipv4/icmp_echo_ignore_broadcasts

# 
# Don't accept source routed packets. 
#
echo "0" > /proc/sys/net/ipv4/conf/all/accept_source_route

#
# We don't like IP spoofing,
#
if [ -f /proc/sys/net/ipv4/conf/all/rp_filter ]; then
    # These two are redundant but I'll kepp'em here for now.
    # Will remind me that I can add the first one somewhere smart later.
    echo "1" > /proc/sys/net/ipv4/conf/default/rp_filter
    echo "1" > /proc/sys/net/ipv4/conf/all/rp_filter

#   while read filter
#    do 
#     echo "1" > $filter
#   done < `find /proc/sys/net/ipv4/conf -name rp_filter -print`
else
    echo "Anti spoofing is not available."
fi

# 
# nor ICMP redirect,
#

if [ -f /proc/sys/net/ipv4/conf/all/accept_redirects ]; then
    echo "0" > /proc/sys/net/ipv4/conf/default/accept_redirects
    echo "0" > /proc/sys/net/ipv4/conf/all/accept_redirects
else
    echo "Anti spoofing is not available."
fi

#stop arp request from other interfaces
for i in /proc/sys/net/ipv4/conf/*; do
    echo 1 > $i/arp_filter
done

#
# Enable bad error message protection.
#
echo "1" > /proc/sys/net/ipv4/icmp_ignore_bogus_error_responses 

#
# Rules set, we can enable forwarding in the kernel.
#
echo "1" > /proc/sys/net/ipv4/ip_forward
