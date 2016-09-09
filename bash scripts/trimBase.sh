#!/bin/sh
trimBase(){
  while read aPath; do
    echo ""${aPath#$1}
  done
}
