#!/bin/sh
linkFiles(){
  $i=0
  
  while read aFile; do
    echo ""$path|sed s/'(^.*)([^/]*)$'/'<a href="'aFile'">'\2'<\a>'/
  done
  #return 1
}
