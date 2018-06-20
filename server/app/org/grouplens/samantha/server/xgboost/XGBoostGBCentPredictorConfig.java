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

package org.grouplens.samantha.server.xgboost;

import com.fasterxml.jackson.databind.JsonNode;

import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractor;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.xgboost.XGBoostGBCent;
import org.grouplens.samantha.modeler.xgboost.XGBoostGBCentProducer;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.featurizer.FeatureExtractorConfig;
import org.grouplens.samantha.server.featurizer.FeatureExtractorListConfigParser;
import org.grouplens.samantha.server.featurizer.FeaturizerConfigParser;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.*;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class XGBoostGBCentPredictorConfig implements PredictorConfig {
    private final Configuration config;
    private final String svdfeaPredictorName;
    private final String svdfeaModelName;
    private final Injector injector;
    private final String modelName;
    private final String modelFile;
    private final List<String> treeFeatures;
    private final List<FeatureExtractorConfig> treeExtractorsConfig;
    private final Configuration daosConfig;
    private final List<Configuration> expandersConfig;
    private final Configuration methodConfig;
    private final String daoConfigKey;

    private XGBoostGBCentPredictorConfig(String modelName, String svdfeaModelName, String svdfeaPredictorName,
                                         List<String> treeFeatures, List<FeatureExtractorConfig> treeExtractorsConfig,
                                         Configuration daosConfig, List<Configuration> expandersConfig,
                                         Configuration methodConfig, Injector injector, String modelFile,
                                         String daoConfigKey, Configuration config) {
        this.daosConfig = daosConfig;
        this.expandersConfig = expandersConfig;
        this.modelName = modelName;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.injector = injector;
        this.methodConfig = methodConfig;
        this.treeExtractorsConfig = treeExtractorsConfig;
        this.treeFeatures = treeFeatures;
        this.modelFile = modelFile;
        this.daoConfigKey = daoConfigKey;
        this.config = config;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        FeaturizerConfigParser parser = injector.instanceOf(
                FeatureExtractorListConfigParser.class);
        Configuration daosConfig = predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<FeatureExtractorConfig> feaExtConfigs = parser.parse(predictorConfig
                .getConfig(ConfigKey.PREDICTOR_FEATURIZER_CONFIG.get()));
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        return new XGBoostGBCentPredictorConfig(predictorConfig.getString("modelName"),
                predictorConfig.getString("svdfeaModelName"), predictorConfig.getString("svdfeaPredictorName"),
                predictorConfig.getStringList("treeFeatures"), feaExtConfigs, daosConfig, expanders,
                predictorConfig.getConfig("methodConfig"), injector, predictorConfig.getString("modelFile"),
                predictorConfig.getString("daoConfigKey"), predictorConfig);
    }

    private class XGBoostGBCentModelManager extends AbstractModelManager {

        public XGBoostGBCentModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile, null);
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            List<FeatureExtractor> featureExtractors = new ArrayList<>();
            for (FeatureExtractorConfig feaExtConfig : treeExtractorsConfig) {
                featureExtractors.add(feaExtConfig.getFeatureExtractor(requestContext));
            }
            SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
            configService.getPredictor(svdfeaPredictorName, requestContext);
            ModelService modelService = injector.instanceOf(ModelService.class);
            SVDFeature svdfeaModel = (SVDFeature) modelService.getModel(requestContext.getEngineName(),
                    svdfeaModelName);
            XGBoostGBCentProducer producer = injector.instanceOf(XGBoostGBCentProducer.class);
            XGBoostGBCent model = producer.createGBCentWithSVDFeatureModel(modelName,
                    SpaceMode.DEFAULT, treeFeatures,
                    featureExtractors, svdfeaModel);
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            JsonNode reqBody = requestContext.getRequestBody();
            XGBoostGBCent gbCent = (XGBoostGBCent) model;
            LearningData data = PredictorUtilities.getLearningData(gbCent, requestContext,
                    reqBody.get("learningDaoConfig"), daosConfig, expandersConfig,
                    injector, true, null, 128);
            LearningData valid = null;
            if (reqBody.has("validationDaoConfig"))  {
                valid = PredictorUtilities.getLearningData(gbCent, requestContext,
                        reqBody.get("validationDaoConfig"), daosConfig, expandersConfig,
                        injector, false, null, 128);
            }
            LearningMethod method = PredictorUtilities.getLearningMethod(methodConfig, injector, requestContext);
            method.learn(gbCent, data, valid);
            return model;
        }
    }

    public Predictor getPredictor(RequestContext requestContext) {
        ModelManager modelManager = new XGBoostGBCentModelManager(modelName, modelFile, injector);
        XGBoostGBCent model = (XGBoostGBCent) modelManager.manage(requestContext);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new PredictiveModelBasedPredictor(config, model, model,
                daosConfig, injector, entityExpanders, daoConfigKey);
    }
}
