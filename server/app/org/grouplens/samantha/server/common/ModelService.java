/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.server.common;

import org.grouplens.samantha.modeler.space.UncollectableModel;

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
            if (engineNamedModels.get(engineName).containsKey(modelName)) {
                Object model = engineNamedModels.get(engineName).get(modelName);
                if (model instanceof UncollectableModel) {
                    //((UncollectableModel) model).destroyModel(); //TODO: problematic, see below
                }
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
                Object model = engineNamedModels.get(engineName).get(modelName);
                if (model instanceof UncollectableModel) {
                    //((UncollectableModel) model).destroyModel(); TODO: problematic when sharing resources in different model names
                }
                engineNamedModels.get(engineName).remove(modelName);
            }
        } finally {
            writeLock.unlock();
        }
    }
}
