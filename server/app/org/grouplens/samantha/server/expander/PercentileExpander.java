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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.model.PercentileModel;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.grouplens.samantha.server.common.*;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.dao.ExpandedEntityDAO;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class PercentileExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(PercentileExpander.class);
    private final List<String> attrNames;
    private final PercentileModel percentileModel;

    public PercentileExpander(List<String> attrNames, PercentileModel percentileModel) {
        this.attrNames = attrNames;
        this.percentileModel = percentileModel;
    }

    static private class PercentileModelManager extends AbstractModelManager {
        private final List<String> attrs;
        private final int maxNumValues;
        private final double sampleRate;
        private final Configuration attr2config;
        private final String daoConfigKey;
        private final Configuration daoConfigs;

        public PercentileModelManager(String modelName, String modelFile, Injector injector,
                                      List<String> attrs, int maxNumValues, double sampleRate,
                                      Configuration attr2config, String daoConfigKey,
                                      Configuration daoConfigs) {
            super(injector, modelName, modelFile, new ArrayList<>());
            this.attrs = attrs;
            this.maxNumValues = maxNumValues;
            this.sampleRate = sampleRate;
            this.attr2config = attr2config;
            this.daoConfigKey = daoConfigKey;
            this.daoConfigs = daoConfigs;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            PercentileModel percentileModel = (PercentileModel) model;
            attrs.parallelStream().forEach(attr -> {
                Configuration config = attr2config.getConfig(attr);
                List<Configuration> expanderConfigs = ExpanderUtilities.getEntityExpandersConfig(config);
                List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                        expanderConfigs, injector);
                EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                        requestContext.getRequestBody().get(daoConfigKey), injector);
                EntityDAO expanded = new ExpandedEntityDAO(expanders, entityDAO, requestContext);
                percentileModel.buildModel(attr, config.getInt("numValues"), expanded);
                expanded.close();
            });
            return model;
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
            return new PercentileModel(modelName, maxNumValues, sampleRate, indexSpace, variableSpace);
        }
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String modelName = expanderConfig.getString("modelName");
        String modelFile = expanderConfig.getString("modelFile");
        List<String> attrNames = JsonHelpers.getOptionalStringList(reqBody,
                expanderConfig.getString("attrNamesKey"),
                expanderConfig.getStringList("attrNames"));
        Configuration daoConfigs = expanderConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        double sampleRate = 1.0;
        if (expanderConfig.asMap().containsKey("sampleRate")) {
            sampleRate = expanderConfig.getDouble("sampleRate");
        }
        int maxNumValues = 100;
        if (expanderConfig.asMap().containsKey("maxNumValues")) {
            maxNumValues = expanderConfig.getInt("maxNumValues");
        }
        ModelManager modelManager = new PercentileModelManager(modelName, modelFile, injector,
                attrNames, maxNumValues, sampleRate, expanderConfig.getConfig("attrName2Config"),
                expanderConfig.getString("daoConfigKey"), daoConfigs);
        PercentileModel model = (PercentileModel) modelManager.manage(requestContext);
        return new PercentileExpander(expanderConfig.getStringList("attrNames"), model);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (String attrName : attrNames) {
            for (ObjectNode entity : initialResult) {
                double val = 0.0;
                if (entity.has(attrName)) {
                    val = entity.get(attrName).asDouble();
                } else {
                    logger.warn("The attribute {} to compute percentile is not present: {}",
                            attrName, entity.toString());
                }
                entity.put(attrName + "Percentile", percentileModel.getPercentile(attrName, val));
            }
        }
        return initialResult;
    }
}
