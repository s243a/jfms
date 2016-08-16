#!/bin/sh
#Puppy Linux
#version 1.1   August 2011

cd /root/my-documents
mkdir -p Screenshots
mkdir -p /root/.config/screenshotbrowser
cd /root
if [ -e /root/.config/screenshotbrowser/bingo ] ;then
   exec mtpaint    /root/my-documents/Screenshots/* &
exit 0   
fi

###############################################
#                                             #
#                C R E A T E                  #
#                                             #
###############################################

create(){
if [ $CHECKBOX = "true" ]; then
touch  /root/.config/screenshotbrowser/bingo
fi
}


export -f create

###############################################
#                                             #
#               M A I N   G U I               #
#                                             #
###############################################

export openScreenshot="	
<window title=\"Browser\">
	
  <frame   Choose an image in 
  the folder 'Screenshots'.
  
  Use the up and down arrow to 
  choose an image.
  
  Tap on HOME key to change 
  the function of the window.
>
  <pixmap><input file>/usr/share/pixmaps/Pictureviewer.png</input></pixmap>
  <text>   <label>  " " </label></text>
 
<checkbox>
   
    <label>Do not show this again</label>
    <variable>CHECKBOX</variable>
</checkbox>
    <text><label>\"\"</label></text>

<hbox homogeneous=\"true\"> 
    <button>
    <input file icon=\"gtk-cancel\"></input>
    <label>  Cancel  </label>
	<action>exit: Screenshot</action>
</button> 
 
<button>
    <input file icon=\"gtk-apply\"></input>
    <label>  Start  </label>
    <action>exec mtpaint    /root/my-documents/Screenshots/* &</action>
    <action>create</action>
	<action>exit: Screenshot</action>
</button>
  
</hbox>

 
  </frame>
</window>"


I=$IFS; IFS=""
for STATEMENTS in  $(gtkdialog3 --program=openScreenshot --center ); do
	eval $STATEMENTS
done
IFS=$I



