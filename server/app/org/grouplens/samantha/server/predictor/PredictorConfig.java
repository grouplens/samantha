package org.grouplens.samantha.server.predictor;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

//TODO: add model evaluation support for all kinds of predictors
public interface PredictorConfig {
    Predictor getPredictor(RequestContext requestContext);
    static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                              Injector injector) {return null;}
}
