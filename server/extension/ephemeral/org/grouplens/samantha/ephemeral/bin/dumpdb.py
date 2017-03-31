import subprocess
import random
import os
import sys
import datetime

def tsPrint(*args, **kwargs):
    timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
    print(timestamp, '-', sys.argv[0], '-', *args, **kwargs)

def dump(path, dbhost, dbname):
    os.makedirs(path, exist_ok=True)
    
    ids_path = os.path.join(path, "ids.tsv")
    full_path = os.path.join(path, "full.tsv")
    full_rand_path = os.path.join(path, "full.rand.tsv")
    train_path = os.path.join(path, "train.tsv")
    val_path = os.path.join(path, "val.tsv")
    
    # Dump movie ids to a temp file
    tsPrint("Dumping movie ids to disk")
    subprocess.check_call("""mysql -h %s -u readonly -D %s -B -e "SELECT movieId FROM movie_data WHERE movieStatus = 2 AND rowType = 11" > %s""" % (dbhost, dbname, ids_path), shell=True)
    
    with open(ids_path) as fp:
        ids = set([int(i.strip()) for i in list(fp)[1:]])
    
    # Dump mysql database to temp file
    tsPrint("Dumping ratings data to disk")
    subprocess.check_call("""mysql -h %s -u readonly -D %s -B -e "SELECT userId, movieId, rating FROM user_rating_pairs WHERE rating > 0.0" > %s""" % (dbhost, dbname, full_path), shell=True)
    
    tsPrint("Randomizing data order")
    subprocess.check_call("""{ head -1 %s ; tail -n +2 %s | shuf ; } | cat > %s """ % (full_path, full_path, full_rand_path), shell=True)
    
    ignored_ids = set([])
    ignored_count = 0
    
    # Open the temp file, and split it into train and validation files
    tsPrint("Partitioning data into train and validation sets")
    with open(full_rand_path) as fp:
        trainfp = open(train_path, 'w')
        valfp = open(val_path, 'w')
    
        # Read the header and write it to both files.
        header = fp.readline()
        trainfp.write(header)
        valfp.write(header)
        
        i = 0;
        while True:
            line = fp.readline()
            if not line: break
            
            movie_id = int(line.split('\t')[1])
            if movie_id not in ids:
                ignored_ids.add(movie_id)
                ignored_count += 1
                continue
            
            # Since the file is already randomized, we can just put every
            # 10th item in the validation set
            i += 1;
            if i % 10 == 0:
                valfp.write(line)
            else:
                trainfp.write(line)
    
    tsPrint("Ignored %d ratings for %d movie ids" % (ignored_count, len(ignored_ids)))

    # Delete the temp file
    tsPrint("Deleting temp files")
    os.remove(full_rand_path)
    #os.remove(ids_path)

arguments = sys.argv[1:]
if len(arguments) != 3:
    tsPrint("dumpdb requires three positional arguments: path, dbhost, dbname")
    exit()
path, dbhost, dbname = arguments

dump(path, dbhost, dbname)