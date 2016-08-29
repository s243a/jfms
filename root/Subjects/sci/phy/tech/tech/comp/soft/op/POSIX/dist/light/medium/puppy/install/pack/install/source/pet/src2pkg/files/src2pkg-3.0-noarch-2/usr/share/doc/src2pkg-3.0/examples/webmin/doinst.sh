#!/bin/sh

pkg_postinst () {
	local crypt=$(grep "^root:" etc/shadow | cut -f 2 -d :)
	crypt=${crypt//\\/\\\\}
	crypt=${crypt//\//\\\/}
	sed -i "s/root:XXX/root:${crypt}/" etc/webmin/miniserv.users
}

config() {
  NEW="$1"
  OLD="$(dirname $NEW)/$(basename $NEW .new)"
  # If there's no config file by that name, mv it over:
  if [ ! -r $OLD ]; then
    mv $NEW $OLD
  elif [ "$(cat $OLD | md5sum)" = "$(cat $NEW | md5sum)" ]; then
    # toss the redundant copy
    rm $NEW
  fi
  # Otherwise, we leave the .new copy for the admin to consider...
}

# Keep same perms on rc.webmin.new:
if [ -e etc/rc.d/rc.webmin ]; then
  cp -a etc/rc.d/rc.webmin etc/rc.d/rc.webmin.new.incoming
  cat etc/rc.d/rc.webmin.new > etc/rc.d/rc.webmin.new.incoming
  mv etc/rc.d/rc.webmin.new.incoming etc/rc.d/rc.webmin.new
fi

pkg_postinst
config /etc/rc.d/rc.webmin.new

if [[ "$(grep 'rc.webmin' /etc/rc.d/rc.local)" = "" ]] ; then

echo "" >> /etc/rc.d/rc.local
echo "# Start Webmin:" >> /etc/rc.d/rc.local
echo "if [ -x /etc/rc.d/rc.webmin ]; then" >> /etc/rc.d/rc.local
echo "   /etc/rc.d/rc.webmin start" >> /etc/rc.d/rc.local
echo "fi" >> /etc/rc.d/rc.local

fi

echo "Run the program /sbin/webmin-setup as root to"
echo "configure your Webmin installation for use."
sleep 5
