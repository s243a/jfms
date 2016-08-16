#!/bin/sh
# listdd.sh - called by /usr/sbin/listdd_wrapper
# Purpose: list dynamic library dependencies
# Roger D. Grider © 2014 GPL3

export TEXTDOMAIN=listdd
export OUTPUT_CHARSET=UTF-8

export APPDIR="/usr/local/ListDD"
export VER=1.1

# ---------
# FUNCTIONS
# ---------

case "$1" in
-summary)
if [ "$FILEPATH" ]; then
  ldd "$FILEPATH" | awk '{$NF=""; print $0}' | sed 's/=> not/=> not found/g' > /tmp/listdd.info
else
  echo "$(gettext 'No file selected')" > /tmp/listdd.info
fi
exit 0
;;

-complete)
if [ "$FILEPATH" ]; then
  ldd -v "$FILEPATH" | sed 's/^	//g' > /tmp/listdd.info
else
  echo "$(gettext 'No file selected')" > /tmp/listdd.info
fi
exit 0
;;

-unused)
if [ "$FILEPATH" ]; then
  ldd -u -r "$FILEPATH" | sed 's/^	//g' > /tmp/listdd.info
  [ ! "`cat /tmp/listdd.info`" ] && echo "$(gettext 'No unused dependencies')" > /tmp/listdd.info
else
  echo "$(gettext 'No file selected')" > /tmp/listdd.info
fi
exit 0
;;

-missing)
if [ "$FILEPATH" ]; then
  if [ ! "`grep 'not a dynamic' /tmp/listdd.info`" ]; then
   ldd -v "$FILEPATH" | grep 'not found' | sed -e 's/^[ \t]*//' > /tmp/listdd.info
   [ ! "`cat /tmp/listdd.info`" ] && echo "$(gettext 'No missing dependencies')" > /tmp/listdd.info
  fi
else
  echo "$(gettext 'No file selected')" > /tmp/listdd.info
fi
exit 0
;;

-help)
  echo "$(gettext "ListDD is a graphical front-end for the ldd command which lists
dynamic dependencies of binary executables and library files.

Binary files are typically found in /usr/bin and /usr/sbin,
and library files of interest are in /usr/lib and /usr/lib64.

ListDD (ldd) does not support script executables, such as those
in /usr/local/bin, and simply defines them as 'not a dynamic'.

You can load files in ListDD with the 'File Selector' button,
or drag & drop from ROX-Filer. Alternatively, use the context
right-click menu of ROX-Filer and Thunar to select and load.

The 'Summary' button provides a basic report of shared libraries
required by the selected file, and the 'Complete' button provides
a verbose report which includes symbol versioning information.

The 'Unused' button enumerates unused direct dependencies, and
the 'Missing' button reports dependencies that are required by
a program, but are not currently installed.

Finally, the 'Packages' button calls the Puppy package manager
to evaluate required dependencies of installed applications.

ListDD-$VER
© 2014 GPL3
Roger D. Grider")" > /tmp/listdd.info
exit 0
;;
esac

# define gtkdialog
if [ "`which gtkdialog4`" ]; then
 GTKDIALOG=gtkdialog4
elif [ "`which gtkdialog3`" ]; then
 GTKDIALOG=gtkdialog3
else
 GTKDIALOG=gtkdialog
fi

# define title-bar icon
ln -sf /usr/local/ListDD/icons/listdd.svg /usr/share/icons/hicolor/48x48/apps && gtk-update-icon-cache -f -i /usr/share/icons/hicolor 2>/dev/null

# auto-adjust header text color for light or dark GTK themes
if [ "`grep 'Stardust_dark' $HOME/.gtkrc-2.0 2>/dev/null`" ]; then #gold text for unique Stardust dark-grey-orange themes
  COLOR=#D7B740
else #light-blue text for common dark themes, otherwise medium blue
  [ "`grep -Ei 'black|dark|night' $HOME/.gtkrc-2.0 2>/dev/null`" ] && COLOR=#84aad9 || COLOR=#3272C0
fi
export COLOR

# define GUI fonts
echo "style \"specialmono\"
{
  font_name=\"Mono\"
}
widget \"*mono\" style \"specialmono\"
class \"GtkText*\" style \"specialmono\"

style \"specialframe\"
{
  font_name=\"Sans bold\"
#  fg[NORMAL]=\"grey40\"
}
widget \"*.GtkFrame.GtkLabel\" style \"specialframe\"
class \"*.GtkFrame.GtkLabel\" style \"specialframe\"" > /tmp/gtkrc_mono

export GTK2_RC_FILES=/tmp/gtkrc_mono:/root/.gtkrc-2.0

# define package-dependency check
if [ "`grep -a "package manager for Puppy Linux" /usr/sbin/petget`" ]; then
   CHECK_DEPS=""
else
   CHECK_DEPS="
   <button height-request=\"35\" width-request=\"100\" tooltip-text=\" $(gettext 'Check dependencies of installed packages') \" use-underline=\"true\">
    <label>$(gettext '_Packages')</label>
    <action>/usr/local/petget/check_deps.sh &</action>
   </button>"
fi

# define initial summary information
ldd "$FILEPATH" 2>/dev/null | awk '{$NF=""; print $0}' > /tmp/listdd.info
[ ! "`cat /tmp/listdd.info`" ] && echo "$(gettext 'Select a binary executable or library file...')" > /tmp/listdd.info

# Backup recently-used.xbel
if [ -r "${HOME}/.recently-used.xbel" ] && [ ! -r "${HOME}/.local/share/recently-used.xbel" ]; then
  ln -sf ${HOME}/.recently-used.xbel ${HOME}/.local/share/recently-used.xbel
fi
[ -r $HOME/.local/share/recently-used.xbel ] && cp -f $HOME/.local/share/recently-used.xbel $HOME/.local/share/recently-used.xbel.bak

# main dialog
export LISTDD_DIALOG="
<window title=\"LDD\" icon-name=\"listdd\" resizable=\"true\">
 <vbox>

  <text use-markup=\"true\" tooltip-text=\" $(gettext 'List Dynamic Dependencies') \"><label>\"<b><span size='"'x-large'"'>List</span><span size='"'x-large'"' color='"$COLOR"'>DD</span></b>\"</label></text>

  <frame $(gettext 'Dependencies')>
   <edit name=\"mono\" editable=\"false\">
    <variable>INFO</variable>
    <input file>/tmp/listdd.info</input>
    <height>360</height>
    <width>660</width>
   </edit>
  <hbox>
  <text use-markup=\"true\"><label>\"<b>$(gettext 'File Path:')</b>\"</label></text>
  <entry height-request=\"30\" tooltip-text=\" $(gettext 'Select a binary executable or library file') \" fs-action=\"newfile\" fs-folder=\"/usr\" fs-title=\" $(gettext 'ListDD File Selector') \" secondary-icon-stock=\"gtk-clear\" secondary-icon-tooltip-text=\" $(gettext 'Clear entry') \">
    <variable>FILEPATH</variable>
    <input>echo $FILEPATH</input>
    <action signal=\"changed\">. listdd.sh -summary</action>
    <action signal=\"changed\">refresh:INFO</action>
    <action signal=\"secondary-icon-release\">clear:FILEPATH</action>
    <action signal=\"secondary-icon-release\">clear:INFO</action>
  </entry>
  <button relief=\"1\" height-request=\"30\" width-request=\"30\" tooltip-text=\" $(gettext 'File selector') \">	
    <input file stock=\"gtk-directory\"></input>
    <action>fileselect:FILEPATH</action>
  </button>
  <button relief=\"1\" height-request=\"30\" width-request=\"30\" tooltip-text=\" $(gettext 'Help') \">	
    <input file>$APPDIR/icons/help.svg</input><height>18</height><width>18</width>
    <action>. listdd.sh -help</action>
    <action>refresh:INFO</action>
  </button>
  </hbox>
  </frame>

  <hbox>
   <button has-focus=\"true\" height-request=\"35\" width-request=\"100\" tooltip-text=\" $(gettext 'Summary report') \" use-underline=\"true\">
    <label>$(gettext '_Summary')</label>
    <action>. listdd.sh -summary</action>
    <action>refresh:INFO</action>
   </button>
   <button height-request=\"35\" width-request=\"100\" tooltip-text=\" $(gettext 'Verbose report') \" use-underline=\"true\">
    <label>$(gettext '_Complete')</label>
    <action>. listdd.sh -complete</action>
    <action>refresh:INFO</action>
   </button>
   <button height-request=\"35\" width-request=\"100\" tooltip-text=\" $(gettext 'Unused direct dependencies') \" use-underline=\"true\">
    <label>$(gettext '_Unused')</label>
    <action>. listdd.sh -unused</action>
    <action>refresh:INFO</action>
   </button>
   <button height-request=\"35\" width-request=\"100\" tooltip-text=\" $(gettext 'Absent / Missing dependencies') \" use-underline=\"true\">
    <label>$(gettext '_Missing')</label>
    <action>. listdd.sh -missing</action>
    <action>refresh:INFO</action>
   </button>
   $CHECK_DEPS
   <button height-request=\"35\" width-request=\"100\" use-underline=\"true\">
    <label>$(gettext 'E_xit')</label>
    <input file stock=\"gtk-close\"></input>
    <action>EXIT:quit_now</action>
   </button>
   <button space-expand=\"true\" space-fill=\"true\" visible=\"false\"><label>\" \"</label></button>
  </hbox>
 </vbox> 
</window>"

$GTKDIALOG --geometry +50+50 -p LISTDD_DIALOG > /dev/null 2>&1 </dev/null
unset LISTDD_DIALOG
rm -f /tmp/listdd.info
[ -r $HOME/.local/share/recently-used.xbel.bak ] && cp -f $HOME/.local/share/recently-used.xbel.bak $HOME/.local/share/recently-used.xbel

exit 0