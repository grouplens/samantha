package org.grouplens.samantha.server.config;

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
            indexer.index(requestContext);
            indexer.notifyDataSubscribers(requestContext);
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
