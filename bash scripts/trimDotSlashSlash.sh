#!/bin/bash
trimDotSlashSlash(){
  while read aPath; do
  #For info on string subtitution see: http://www.gnu.org/software/bash/manual/bashref.html#Shell-Parameter-Expansion
  #echo ""${${${aPath#.}#/}%/} ##Why can't it be this easy????
  aPath=${aPath#.}; #Trim leading .
  #aPath=${aPath#/}; #Trim Reading /
  aPath=${aPath%/}; #Trim trailing /
  echo ""$aPath
  done 
}
