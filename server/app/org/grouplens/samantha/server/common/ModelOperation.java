/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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
