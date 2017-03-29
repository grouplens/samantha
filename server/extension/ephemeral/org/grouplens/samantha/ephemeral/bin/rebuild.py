"""
Send a request to samantha to rebuild the models.

"""

import threading
import os
import sys

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

def rebuildModels(host):
    predictorUrl = host + "ephemeral/predictor/model"
    retrieverUrl = host + "ephemeral/retriever/model"
    path = '/Users/Taavi/Research/Ephemeral/data/' # TODO: Set data file path
    
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
    print("Building ephemeral predictor model on %s" % host)
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
    print("Updating ephemeral predictor model with validation data on %s" % host)
    postDataToUrl(predictorUrl, data)
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "DUMP",
    }
    print("Dumping ephemeral predictor model to disk on %s"%  host)
    postDataToUrl(predictorUrl, data)
    
    # Build and dump the ephemeral retriever model
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "BUILD",
    }
    print("Building ephemeral retriever model on %s" % host)
    postDataToUrl(retrieverUrl, data)
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "DUMP",
    }
    print("Dumping ephemeral retriever model to disk on %s" % host)
    postDataToUrl(retrieverUrl, data)
    
    print("All done on %s", host)


# TODO: Run on both hosts.
hosts = sys.argv[1:] or ["http://localhost:9100/"]
threads = []
for host in hosts:
    t = threading.Thread(target=rebuildModels, args=[host])
    t.start()
    threads.append(t)

for thread in threads:
    thread.join()

