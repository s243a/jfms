#!/bin/sh

( cd usr/libexec/src2pkg/bin ; rm -rf install )
( cd usr/libexec/src2pkg/bin ; ln -sf ginstall install )
