# Install the info files for this package
if [ -x /usr/bin/install-info ]
then
   /usr/bin/install-info --info-dir=/usr/info /usr/info/mysql.info.gz 2>/dev/null
fi
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
config etc/rc.d/rc.mysqld.new
#

( cd usr/lib/mysql ; rm -rf libmysqlclient_r.so.15 )
( cd usr/lib/mysql ; ln -sf libmysqlclient_r.so.15.0.0 libmysqlclient_r.so.15 )
( cd usr/lib/mysql ; rm -rf libmysqlclient.so )
( cd usr/lib/mysql ; ln -sf libmysqlclient.so.15.0.0 libmysqlclient.so )
( cd usr/lib/mysql ; rm -rf libmysqlclient.so.15 )
( cd usr/lib/mysql ; ln -sf libmysqlclient.so.15.0.0 libmysqlclient.so.15 )
( cd usr/lib/mysql ; rm -rf libmysqlclient_r.so )
( cd usr/lib/mysql ; ln -sf libmysqlclient_r.so.15.0.0 libmysqlclient_r.so )
( cd usr/lib ; rm -rf libmysqlclient_r.so.15 )
( cd usr/lib ; ln -sf mysql/libmysqlclient_r.so.15 libmysqlclient_r.so.15 )
( cd usr/lib ; rm -rf libmysqlclient.so )
( cd usr/lib ; ln -sf mysql/libmysqlclient.so libmysqlclient.so )
( cd usr/lib ; rm -rf libmysqlclient.so.15 )
( cd usr/lib ; ln -sf mysql/libmysqlclient.so.15 libmysqlclient.so.15 )
( cd usr/lib ; rm -rf libmysqlclient_r.so )
( cd usr/lib ; ln -sf mysql/libmysqlclient_r.so libmysqlclient_r.so )
