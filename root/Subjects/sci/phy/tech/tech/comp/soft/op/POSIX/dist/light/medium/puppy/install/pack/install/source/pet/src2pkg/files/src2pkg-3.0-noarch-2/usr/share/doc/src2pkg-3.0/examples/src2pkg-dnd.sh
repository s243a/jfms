#!/bin/bash
# src2pkg-dnd Copyright 2008-2010 Gilbert Ashley <amigo@ibiblio.org>

# Aww man, you gotta be kidding!? Drag-n-drop package making?
# Come on now -how lazy can you get? -Or maybe you just have lots
# of packages to make or hate typing in those long tarball names...
# Anyway, this is a small wrapper to the src2pkg package making
# software. It lets you use drag and drop to build packages! Simply
# use the mouse to drag one or more tarballs onto the icon for this
# program. You can place this program in the regular path, like
# /usr/bin, but it isn't really necessary. It's really better to make
# a copy of this program in your $HOME directory. then you can
# just drop archives on it, or even better, create an icon for it on your
# desktop so that... well so you can be even more lazy!
# By having a copy of this in your $HOME directory, you can edit
# it to set the options so that it is most useful to you.

# Set your favorite src2pkg options here for use with all archives
# best candidates for use here are: -A, -N, -W, -C, -T, R  or none

# Here's a few examples -starting from the most sensible to the craziest
# I personally use this: ARCHIVE_OPTIONS='-A'
# Just use this to write new starting scripts for you, without doing more:
ARCHIVE_OPTIONS='-N -Q -V'
# Use this to just test builds without trying to create a package
# ARCHIVE_OPTIONS='-T'
# Use this to auto-build and write a script if the build is successful
#ARCHIVE_OPTIONS='-A -R'
# Use this to create a package and install right away -man are you nuts?
# ARCHIVE_OPTIONS='-I -W'

# 
# This lets you use different options for when a *.src2pkg (or *.src2pkg.auto)
# script is dropped on the icon. 
# Useful options here might be -W, -V, -VV
SCRIPT_OPTIONS='-VV -A'

# src2pkg will run in a new xterm for each package.
# set this to '1' if you want the program to pause  after each build
# setting to anything else means src2pkg will just keep going
PAUSE=1

HELP_TEXT="	src2pkg-dnd  -A drag-n-drop frontend to src2pkg

  This utility allows you to compile sources and create packages from them
  by simply dragging the source archive and releasing it on the icon for this
  program. The best way to use this tool is to create a copy of it in your 
  \$HOME directory. Then open it in your favorite text editor and set any
  default options you want at the top of the file. Then create a shortcut or
  link to it on your desktop. Now, simply dropping one or more archives on the
  icon will cause src2pkg to configure and compile them and then create
  finished packages of them. Each package will built as if you had run src2pkg
  in the same directory as the where the tarball is located. Have fun!
"

if [[ $# = 0 ]] ; then
	if [[ $DISPLAY ]] ; then
		#xterm -hold -e "echo Drop a tarball on me to have it compiled and packaged."
		xterm -title "src2pkg-dnd - Help" -hold -e echo "$HELP_TEXT"
		exit 0
	else
		echo "$HELP_TEXT"
		exit 0
	fi
elif [[ $1 = '--help' ]] || [[ $1 = '-h' ]] ; then
	echo "$HELP_TEXT"
	exit 0
else
	for f in "$@" ; do
		path_to_file=$(dirname $f)
		filename=$(basename $f)
		#if [[ "$ARCHIVE_OPTIONS" != "$SCRIPT_OPTIONS" ]] ; then
		case $filename in
			*.SlackBuild) SRC2PKG_OPTIONS="$SCRIPT_OPTIONS --convert-simple" ;;
			*.src2pkg|*.src2pkg.auto) SRC2PKG_OPTIONS=$SCRIPT_OPTIONS ;;
			*) SRC2PKG_OPTIONS=$ARCHIVE_OPTIONS ;;
		esac
		#fi
		
		if [[ $DISPLAY ]] ; then
			cd $path_to_file ;
			CWD=$(pwd)
			if [[ $PAUSE = 1 ]] ; then
				xterm -title "src2pkg-dnd - $filename" -hold -e "src2pkg $SRC2PKG_OPTIONS $filename ; echo 'Done! Close window to proceed.'"
			else
				xterm -title "src2pkg-dnd - $filename" -e "src2pkg $SRC2PKG_OPTIONS $filename"
			fi
		else
			cd $path_to_file
			src2pkg $SRC2PKG_OPTIONS $filename
		fi
	done
	exit 0
fi
