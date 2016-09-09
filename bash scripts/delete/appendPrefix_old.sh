#!/bin/sh
appendPrefix(){
  while read aPath; do
    echo ""$1$aPath
  done
  return 1
}
