package org.grouplens.samantha.server.common;

import org.grouplens.samantha.server.io.RequestContext;

public enum ModelOperation implements ModelOperator {
    BUILD("BUILD") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.buildModel(requestContext);
        }
    },
    UPDATE("UPDATE") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.updateModel(requestContext);
        }
    },
    DUMP("DUMP") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.dumpModel(requestContext);
        }
    },
    LOAD("LOAD") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.loadModel(requestContext);
        }
    },
    EVALUATE("EVALUATE") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.evaluateModel(requestContext);
        }
    },
    RESET("RESET") {
        public Object operate(ModelManager modelManager, RequestContext requestContext) {
            return modelManager.resetModel(requestContext);
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
