#!/bin/sh
source trimDotSlashSlash
source appendPrefix
source trimBase
listDirs() {
  while getopts "fb" opt; do
    case  $opt in
      f)
        $prefix = pwd
      b)
        shift
        base = $1
     esac
   done
  ls -d ./*/ trimDotSlashSlash | appendPrefix $prefix | trimBase $base
}
