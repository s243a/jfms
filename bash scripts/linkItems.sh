#!/bin/sh
linkItems(){
  while read aPath; do
    echo ""$aPath | sed -re 's/^(.*)\/([^/]*)\/$/\<a href="\1\/\2">\2<\/a>/'
  done
  return 1
}


