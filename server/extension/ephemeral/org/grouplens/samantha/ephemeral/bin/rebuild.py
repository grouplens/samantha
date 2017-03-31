"""
Send a request to samantha to rebuild the models.

"""

import threading
import os
import sys
import datetime

def tsPrint(*args, **kwargs):
    timestamp = datetime.datetime.now().strftime('%Y-%m-%d %H:%M:%S.%f')[:-3]
    print(timestamp, '-', sys.argv[0], '-', *args, **kwargs)

def postDataToUrl(url, data):
    """Helper function which posts JSON to a URL."""
    
    import urllib.request, json
    
    # Construct the request
    req = urllib.request.Request(url, headers={'Content-Type': 'application/json;charset=utf-8'})
    data_as_bytes = json.dumps(data).encode('utf-8')   # needs to be bytes
    req.add_header('Content-Length', len(data_as_bytes))
    
    # Post the request and return the response
    response = urllib.request.urlopen(req, data_as_bytes)
    encoding = response.info().get_content_charset('utf-8')
    return response.info(), response.read().decode(encoding)

def rebuildModels(path, host):
    predictorUrl = host + "ephemeral/predictor/model"
    retrieverUrl = host + "ephemeral/retriever/model"
    
    # Build and dump the ephemeral predictor model
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "BUILD",
       "learningDaoConfig": {
           "filePath": os.path.join(path, "train.tsv"),
           "separator": "\t",
           "entityDaoName": "CSVFileEntityDAO" #RandomCSVFileEntityDAO 
       },
       "validationDaoConfig": {
           "filePath": os.path.join(path, "val.tsv"),
           "separator": "\t",
           "entityDaoName": "CSVFileEntityDAO"
       }
    }
    tsPrint("Building ephemeral predictor model on %s" % host)
    postDataToUrl(predictorUrl, data)
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "UPDATE",
       "updateDaoConfig": {
           "filePath": os.path.join(path, "val.tsv"),
           "separator": "\t",
           "entityDaoName": "CSVFileEntityDAO"
       }
    }
    tsPrint("Updating ephemeral predictor model with validation data on %s" % host)
    postDataToUrl(predictorUrl, data)
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "DUMP",
    }
    tsPrint("Dumping ephemeral predictor model to disk on %s"%  host)
    postDataToUrl(predictorUrl, data)
    
    # Build and dump the ephemeral retriever model
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "BUILD",
    }
    tsPrint("Building ephemeral retriever model on %s" % host)
    postDataToUrl(retrieverUrl, data)
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "DUMP",
    }
    tsPrint("Dumping ephemeral retriever model to disk on %s" % host)
    postDataToUrl(retrieverUrl, data)
    
    tsPrint("All done on %s" % host)


arguments = sys.argv[1:]
if len(arguments) < 2:    
    tsPrint("rebuild requires two positional arguments: path, and host")
    exit()
path, hosts = arguments[0], arguments[1:]

threads = []
for host in hosts:
    t = threading.Thread(target=rebuildModels, args=[path, host])
    t.start()
    threads.append(t)

for thread in threads:
    thread.join()

