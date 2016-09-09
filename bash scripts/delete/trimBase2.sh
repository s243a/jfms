#!/bin/sh
trimBase() {
  $1|sed s/'($2)(.*)'/\2/
}
