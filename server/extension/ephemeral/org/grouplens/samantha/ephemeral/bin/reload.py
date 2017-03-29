"""
Send a request to samantha to reload the models.

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


def reloadModels(host):
    predictorUrl = host + "ephemeral/predictor/model"
    retrieverUrl = host + "ephemeral/retriever/model"
    
    # Reload the ephemeral predictor model
    data = {
       "predictor": "ephemeral-predictor",
       "modelName": "ephemeral-predictor-model",
       "modelOperation": "LOAD",
    }
    print("Loading ephemeral predictor model from disk on %s" % host)
    postDataToUrl(predictorUrl, data)

    # Reload the ephemeral retriever model
    data = {
       "retriever": "ephemeral-retriever",
       "modelName": "ephemeral-retriever-model",
       "modelOperation": "LOAD",
    }
    print("Loading ephemeral retriever model from disk on %s" % host)
    postDataToUrl(retrieverUrl, data)

    print("All done on %s", host)


hosts = sys.argv[1:] or ["http://localhost:9100/"]
threads = []
for host in hosts:
    t = threading.Thread(target=reloadModels, args=[host])
    t.start()
    threads.append(t)

for thread in threads:
    thread.join()

