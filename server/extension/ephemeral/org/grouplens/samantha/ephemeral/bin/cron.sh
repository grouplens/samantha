# Get path of directory containing this script
SCRIPT=$(readlink -f "$0")
DIR=$(dirname "$SCRIPT")

PATH=$1; shift
DBHOST=$1; shift
DBNAME=$1; shift

echo $DIR/dumpdb.py $PATH $DBHOST $DBNAME > $DIR/dumpdb.log
echo $DIR/rebuild.py $PATH "$@" > $DIR/rebuild.log