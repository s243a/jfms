#!/bin/bash

appendPrefix(){
  while read aPath; do
  prefix="`pwd`"
  aPath=${aPath#/}
  echo "$1"\/"$aPath"
  done 
}
