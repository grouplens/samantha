"""
Send a request to samantha to reload the models.

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


def reloadModels(host):
    predictorUrl = host + "ephemeral/predictor/model"
    retrieverUrl = host + "ephemeral/retriever/model"
    
    # Reload the ephemeral predictor model
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "LOAD",
    }
    tsPrint("Loading ephemeral predictor model from disk on %s" % host)
    postDataToUrl(predictorUrl, data)

    # Reload the ephemeral retriever model
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "LOAD",
    }
    tsPrint("Loading ephemeral retriever model from disk on %s" % host)
    postDataToUrl(retrieverUrl, data)

    tsPrint("All done on %s" % host)


arguments = sys.argv[1:]
if not arguments:    
    tsPrint("reload requires one positional argument: host")
    exit()
hosts = arguments

threads = []
for host in hosts:
    t = threading.Thread(target=reloadModels, args=[host])
    t.start()
    threads.append(t)

for thread in threads:
    thread.join()

