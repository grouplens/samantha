package org.grouplens.samantha.server.common;

import org.grouplens.samantha.server.io.RequestContext;

public enum ModelOperation implements ModelOperator {
    BUILD("build") {
        public void operate(ModelManager modelManager, RequestContext requestContext) {
            modelManager.buildModel(requestContext);
        }
    },
    UPDATE("update") {
        public void operate(ModelManager modelManager, RequestContext requestContext) {
            modelManager.updateModel(requestContext);
        }
    },
    DUMP("dump") {
        public void operate(ModelManager modelManager, RequestContext requestContext) {
            modelManager.dumpModel(requestContext);
        }
    },
    LOAD("load") {
        public void operate(ModelManager modelManager, RequestContext requestContext) {
            modelManager.loadModel(requestContext);
        }
    },
    RESET("reset") {
        public void operate(ModelManager modelManager, RequestContext requestContext) {
            modelManager.resetModel(requestContext);
        }
    };

    final private String key;

    ModelOperation(String key) {
        this.key = key;
    }

    public String get() {
        return key;
    }
}
