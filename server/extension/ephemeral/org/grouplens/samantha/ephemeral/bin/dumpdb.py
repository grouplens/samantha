import subprocess
import random
import os
import sys

def dump(path, dbhost, dbname):
    full_path = os.path.join(path, "full.tsv")
    full_rand_path = os.path.join(path, "full.rand.tsv")
    train_path = os.path.join(path, "train.tsv")
    val_path = os.path.join(path, "val.tsv")
    
    # Dump mysql database to temp file
    print("Dumping ratings data to disk")
    subprocess.check_call("""mysql -h %s -u readonly -D %s -B -e "SELECT userId, movieId, rating FROM user_rating_pairs WHERE rating > 0.0" > %s""" % (dbhost, dbname, full_path), shell=True)
    
    print("Randomizing data order")
    subprocess.check_call("""{ head -1 %s ; tail -n +2 %s | shuf ; } | cat > %s """ % (full_path, full_path, full_rand_path), shell=True)
    
    
    # Open the temp file, and split it into train and validation files
    print("Partitioning data into train and validation sets")
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
            
            # Since the file is already randomized, we can just put every
            # 10th item in the validation set
            i += 1;
            if i % 10 == 0:
                valfp.write(line)
            else:
                trainfp.write(line)
    
    # Delete the temp file
    print("Deleting temp files")
    os.remove(full_rand_path)

arguments = sys.argv[1:]
if len(arguments) != 3:
    print("dumpdb requires three positional arguments: path, dbhost, dbname")
    exit()
path, dbhost, dbname = arguments

dump(path, dbhost, dbname)