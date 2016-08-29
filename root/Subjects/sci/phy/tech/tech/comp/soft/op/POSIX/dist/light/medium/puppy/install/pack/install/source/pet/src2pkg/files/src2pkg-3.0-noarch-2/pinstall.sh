#!/bin/sh

# Figure out our ROOT directory
#ROOTDIR=$(pwd)
#unset CHROOT
#if [ "${ROOTDIR}" != "/" ] ; then
#  ROOTDIR="${ROOTDIR}/"
#  CHROOT="chroot ${ROOTDIR} "
  # ${CHROOT} &> /dev/null
#fi

config() {
  NEW="$1"
  OLD="`dirname $NEW`/`basename $NEW .new`"
  # If there's no config file by that name, mv it over:
  if [ ! -r $OLD ]; then
    mv $NEW $OLD
  elif [ "`cat $OLD | md5sum`" = "`cat $NEW | md5sum`" ]; then
    # If they are the same, discard the new one
	rm $NEW
  fi
  # Otherwise, leave the *.new copy
}
# install the conf files
config etc/src2pkg/src2pkg.conf.new
config etc/src2pkg/DONT_PURGE.locales.new
config etc/src2pkg/sysdirs.conf.new

# If src2pkg-helpers is already installed, notify the user if it needs to be updated
# Or if src2pkg-helpers is not installed, notify the user that it needs to be setup
if [ -f usr/libexec/src2pkg/bin/version ] ; then
	INSTALLED_HELPERS_VERSION=`cat usr/libexec/src2pkg/bin/version`
elif [ -f usr/libexec/src2pkg/.helpers-version ] ; then
	INSTALLED_HELPERS_VERSION=`cat usr/libexec/src2pkg/.helpers-version`
elif [ -f var/log/packages/src2pkg-helpers* ] ; then
	INSTALLED_HELPERS_VERSION=`ls var/log/packages/src2pkg-helpers* |cut -f3 -d'-'`
else
	INSTALLED_HELPERS_VERSION=0
fi
if [ -f usr/src/src2pkg/src2pkg-helpers/src2pkg-helpers.src2pkg ] ; then
	CURRENT_HELPERS_VERSION=`grep '^VERSION=' usr/src/src2pkg/src2pkg-helpers/src2pkg-helpers.src2pkg |cut -f 2 -d'='`
fi

if [ -d var/lib/tpkg/packages ] ; then
	ADM_DIR=var/lib/tpkg/packages
elif [ -d var/log/packages ] ; then
	ADM_DIR=var/log/packages
fi

if [ -n $ADM_DIR ] ; then
  # ignore the pre-installation pass if we are being installed using upgradepkg
  if ! [ `ls $ADM_DIR 2> /dev/null |grep src2pkg |grep upgraded` ] ; then
	if [ ! -x usr/libexec/src2pkg/lib/libsentry.so ] || [ "$INSTALLED_HELPERS_VERSION" != "$CURRENT_HELPERS_VERSION" ] ; then
		if [ "$INSTALLED_HELPERS_VERSION" = "0" ] ; then
			echo ""
				echo "  src2pkg uses a small shared library called libsentry"
				echo "  and specific versions of tar and other programs to create"
				echo "  packages. These should be compiled on your system to"
				echo "  ensure the best compatibility, no matter which OS or"
				echo "  version you are running. To compile them, you need"
				echo "  to run the command 'src2pkg --setup' as user 'root'."
				echo ""
		else
				echo "  NOTICE - Your installed version of src2pkg-helpers"
				echo "  src2pkg-helpers needs to be updated. An installed version was "
				echo "  found which doesn't match the current version. To upgrade,"
				echo "  you need to run the command 'src2pkg --setup' as user 'root'."
		fi
		#echo "  This screen will close in 20 seconds, or press CTRL-C to dismiss."
		#sleep 20
		echo "Press any key to continue."
		read -sn 1
	fi
  fi
elif [ "$INSTALLED_HELPERS_VERSION" != "$CURRENT_HELPERS_VERSION" ] ; then
		# this is a non-pkgtool system
		if [ "$INSTALLED_HELPERS_VERSION" = "0" ] ; then
			echo ""
				echo "  src2pkg uses a small shared library called libsentry"
				echo "  and specific versions of tar and other programs to create"
				echo "  packages. These should be compiled on your system to"
				echo "  ensure the best compatibility, no matter which OS or"
				echo "  version you are running. To compile them, you need"
				echo "  to run the command 'src2pkg --setup' as user 'root'."
				echo ""
		else
				echo "  NOTICE - Your installed version of src2pkg-helpers"
				echo "  src2pkg-helpers needs to be updated. An installed version was "
				echo "  found which doesn't match the current version. To upgrade,"
				echo "  you need to run the command 'src2pkg --setup' as user 'root'."
		fi
		#echo "  This screen will close in 20 seconds, or press CTRL-C to dismiss."
		#sleep 20
		echo "Press any key to continue."
		read -sn 1
fi

# update the applications menu database
if [ -x usr/bin/update-desktop-database ] ; then
 usr/bin/update-desktop-database -q usr/share/applications 2> /dev/null
fi

# update the gtk-icon-cache
if [ -x usr/bin/gtk-update-icon-cache ] ; then
 usr/bin/gtk-update-icon-cache --quiet usr/share/icons 2> /dev/null
fi

# don't clobber existing files or links -the user may have customized the setup
# ( cd usr/share/src2pkg/sounds ; rm -f success failure cancel ouch)
if ! [ -e usr/share/src2pkg/sounds/success ] ; then
  ( cd usr/share/src2pkg/sounds ; ln -sf complete-media-burn.ogg success )
fi
if ! [ -e usr/share/src2pkg/sounds/failure ] ; then
  ( cd usr/share/src2pkg/sounds ; ln -sf DefaultCrapOut.wav failure )
fi
if ! [ -e usr/share/src2pkg/sounds/cancel ] ; then
  ( cd usr/share/src2pkg/sounds ; ln -sf DefaultBoing.wav cancel )
fi
if ! [ -e usr/share/src2pkg/sounds/ouch ] ; then
  ( cd usr/share/src2pkg/sounds ; ln -sf DefaultOuch.wav ouch )
fi
