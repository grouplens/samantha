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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.common.PredictiveModel;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class PredictiveModelBasedPredictor extends AbstractPredictor {
    private final PredictiveModel predictiveModel;
    private final Featurizer featurizer;
    private final Configuration config;
    private final List<EntityExpander> entityExpanders;

    public PredictiveModelBasedPredictor(Configuration config,
                                         PredictiveModel predictiveModel,
                                         Featurizer featurizer,
                                         Configuration daoConfigs,
                                         Injector injector,
                                         List<EntityExpander> entityExpanders,
                                         String daoConfigKey) {
        super(config, daoConfigs, daoConfigKey, injector);
        this.predictiveModel = predictiveModel;
        this.featurizer = featurizer;
        this.config = config;
        this.entityExpanders = entityExpanders;
    }


    public List<Prediction> predict(List<ObjectNode> entityList,
                                    RequestContext requestContext) {
        for (EntityExpander expander : entityExpanders) {
            entityList = expander.expand(entityList, requestContext);
        }
        List<LearningInstance> instanceList = new ArrayList<>(entityList.size());
        for (JsonNode entity : entityList) {
            instanceList.add(featurizer.featurize(entity, false));
        }
        List<Prediction> results = new ArrayList<>(entityList.size());
        for (int i=0; i<entityList.size(); i++) {
            ObjectNode entity = entityList.get(i);
            LearningInstance ins = instanceList.get(i);
            double score = predictiveModel.predict(ins);
            results.add(new Prediction(entity, ins, score));
        }
        return results;
    }
}
