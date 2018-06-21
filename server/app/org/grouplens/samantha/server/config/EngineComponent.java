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

package org.grouplens.samantha.server.config;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.RequestContext;

public enum EngineComponent implements ComponentGetter {
    ROUTER("router") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            configService.getRouter(componentName, requestContext);
        }
    },
    SCHEDULER("schedulers") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            throw new BadRequestException("Scheduler does not support component getter.");
        }
    },
    RETRIEVER("retrievers") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            configService.getRetriever(componentName, requestContext);
        }
    },
    PREDICTOR("predictors") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            configService.getPredictor(componentName, requestContext);
        }
    },
    RANKER("rankers") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            configService.getRanker(componentName, requestContext);
        }
    },
    INDEXER("indexers") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            Indexer indexer = configService.getIndexer(componentName, requestContext);
            JsonNode reqBody = requestContext.getRequestBody();
            if (reqBody.has(ConfigKey.INDEX_DATA.get()) && reqBody.get(ConfigKey.INDEX_DATA.get()).asBoolean()) {
                indexer.index(requestContext);
            }
        }
    },
    EVALUATOR("evaluators") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            Evaluator evaluator = configService.getEvaluator(componentName, requestContext);
            evaluator.evaluate(requestContext);
        }
    },
    RECOMMENDER("recommenders") {
        public void getComponent(SamanthaConfigService configService, String componentName,
                                 RequestContext requestContext) {
            String engineName = requestContext.getEngineName();
            configService.getRecommender(engineName, requestContext);
        }
    };

    private final String component;

    EngineComponent(String component) {
        this.component = component;
    }

    public String get() {
        return component;
    }
}
