#!/bin/sh

config() {
  NEW="$1"
  OLD="`dirname $NEW`/`basename $NEW .new`"
  # If there's no config file by that name, mv it over:
  if [ ! -r $OLD ]; then
    mv $NEW $OLD
  elif [ "`cat $OLD | md5sum`" = "`cat $NEW | md5sum`" ]; then # toss the redundant copy
    rm $NEW
  fi
  # Otherwise, we leave the .new copy for the admin to consider...
}
config etc/squid/mime.conf.new
config etc/squid/squid.conf.new
config etc/squid/cachemgr.conf.new
config etc/rc.d/rc.squid.new

chown -R nobody:nobody $PKG_DIR/var/log/squid

if ! [ $(grep rc.squid /etc/rc.d/rc.local |grep -v '#') ] ; then
 echo "" >> /etc/rc.d/rc.local
 echo "if [ -x /etc/rc.d/rc.squid ]; then" >> /etc/rc.d/rc.local
 echo "  . /etc/rc.d/rc.squid start" >> /etc/rc.d/rc.local
 echo "fi" >> /etc/rc.d/rc.local
fi

echo ""
echo " In order to use squid, you need to configure it first."
echo " Read the file /usr/doc/$NAME-$VERSION/HOWTO.Slackware for more info."
echo " After configuring squid and creating the initial cache, you can have"
echo " squid start at boot-time by making /etc/rc.d/rc.squid executable."
sleep 15
