#!/bin/sh
trimPath() {
  $1|sed s/'^.*([^/]*)$'/\1/
}
