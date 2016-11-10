package org.grouplens.samantha.server.common;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Singleton
public class ModelService {
    private Map<String, Map<String, Object>> engineNamedModels = new HashMap<>();
    private final ReentrantReadWriteLock rwl = new ReentrantReadWriteLock();
    private final Lock readLock = rwl.readLock();
    private final Lock writeLock = rwl.writeLock();

    @Inject
    private ModelService() {}

    public boolean hasModel(String engineName, String modelName) {
        readLock.lock();
        try {
            if (engineNamedModels.containsKey(engineName) &&
                    engineNamedModels.get(engineName).containsKey(modelName)) {
                return true;
            } else {
                return false;
            }
        } finally {
            readLock.unlock();
        }
    }

    public Object getModel(String engineName, String modelName) {
        readLock.lock();
        try {
            return engineNamedModels.get(engineName).get(modelName);
        } finally {
            readLock.unlock();
        }
    }

    public void setModel(String engineName, String modelName, Object object) {
        writeLock.lock();
        try {
            if (!engineNamedModels.containsKey(engineName)) {
                engineNamedModels.put(engineName, new HashMap<>());
            }
            engineNamedModels.get(engineName).put(modelName, object);
        } finally {
            writeLock.unlock();
        }
    }

    public void removeModel(String engineName, String modelName) {
        writeLock.lock();
        try {
            if (engineNamedModels.containsKey(engineName) && engineNamedModels
                    .get(engineName).containsKey(modelName)) {
                engineNamedModels.get(engineName).remove(modelName);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
