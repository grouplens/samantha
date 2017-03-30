#!/bin/sh
if [ "$#" -lt 4 ]; then
  echo "Usage: $0 datadir dbhost dbname samantha_host(s)..." >&2
  exit 1
fi
if ! [ -e "$1" ]; then
  echo "$1 not found" >&2
  exit 1
fi
if ! [ -d "$1" ]; then
  echo "$1 not a directory" >&2
  exit 1
fi

# Get path of directory containing this script
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")

DATAPATH=$1; shift
DBHOST=$1; shift
DBNAME=$1; shift

echo $DIR/dumpdb.py $DATAPATH $DBHOST $DBNAME > $DIR/dumpdb.log
echo $DIR/rebuild.py $DATAPATH "$@" > $DIR/rebuild.log