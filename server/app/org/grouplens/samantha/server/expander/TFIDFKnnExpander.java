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

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.knn.TFIDFKnnModel;
import org.grouplens.samantha.modeler.model.*;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelManager;
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
import java.util.Map;

public class TFIDFKnnExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(TFIDFKnnExpander.class);
    private final TFIDFKnnModel tfidfKnnModel;
    private final int numUsedNeighbors;
    final private String scoreAttr;
    final private List<String> itemAttrs;
    final private List<String> feaAttrs;

    public TFIDFKnnExpander(TFIDFKnnModel tfidfKnnModel, int numUsedNeighbors,
                            List<String> itemAttrs, List<String> feaAttrs, String scoreAttr) {
        this.tfidfKnnModel = tfidfKnnModel;
        this.numUsedNeighbors = numUsedNeighbors;
        this.itemAttrs = itemAttrs;
        this.feaAttrs = feaAttrs;
        this.scoreAttr = scoreAttr;
    }

    static private class TFIDFKnnModelManager extends AbstractModelManager {
        final private List<String> itemAttrs;
        final private List<String> feaAttrs;
        final private int numNeighbors;
        private final String daoConfigKey;
        private final Configuration config;
        private final Configuration daoConfigs;

        public TFIDFKnnModelManager(String modelName, String modelFile, Injector injector,
                                    List<String> itemAttrs, List<String> feaAttrs, int numNeighbors,
                                    Configuration config, String daoConfigKey, Configuration daoConfigs) {
            super(injector, modelName, modelFile, new ArrayList<>());
            this.itemAttrs = itemAttrs;
            this.feaAttrs = feaAttrs;
            this.numNeighbors = numNeighbors;
            this.daoConfigKey = daoConfigKey;
            this.daoConfigs = daoConfigs;
            this.config = config;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            TFIDFKnnModel tfidfKnnModel = (TFIDFKnnModel) model;
            List<Configuration> expanderConfigs = ExpanderUtilities.getEntityExpandersConfig(config);
            List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                    expanderConfigs, injector);
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), injector);
            EntityDAO expanded = new ExpandedEntityDAO(expanders, entityDAO, requestContext);
            tfidfKnnModel.buildModel(expanded);
            expanded.close();
            return model;
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
            return new TFIDFKnnModel(modelName, itemAttrs, feaAttrs, numNeighbors, indexSpace, variableSpace);
        }
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        String modelName = expanderConfig.getString("modelName");
        String modelFile = expanderConfig.getString("modelFile");
        Integer numNeighbors = expanderConfig.getInt("numNeighbors");
        if (numNeighbors == null) {
            numNeighbors = 1;
        }
        Integer numUsedNeighbors = expanderConfig.getInt("numUsedNeighbors");
        if (numUsedNeighbors == null) {
            numUsedNeighbors = numNeighbors;
        }
        List<String> itemAttrs = expanderConfig.getStringList("itemAttrs");
        List<String> feaAttrs = expanderConfig.getStringList("feaAttrs");
        Configuration daoConfigs = expanderConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        ModelManager modelManager = new TFIDFKnnModelManager(modelName, modelFile, injector,
                itemAttrs, feaAttrs, numNeighbors, expanderConfig,
                expanderConfig.getString("daoConfigKey"), daoConfigs);
        TFIDFKnnModel model = (TFIDFKnnModel) modelManager.manage(requestContext);
        return new TFIDFKnnExpander(model, numUsedNeighbors, itemAttrs, feaAttrs,
                expanderConfig.getString("scoreAttr"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            String key = FeatureExtractorUtilities.composeConcatenatedKey(entity, itemAttrs);
            if (tfidfKnnModel.hasKey(key)) {
                RealVector features = tfidfKnnModel.getKeyVector(key);
                for (int i=0; i<numUsedNeighbors && 2*i+1<features.getDimension(); i++) {
                    String feature = tfidfKnnModel.getKeyByIndex((int)features.getEntry(i*2));
                    Map<String, String> attrs = FeatureExtractorUtilities.decomposeKey(feature);
                    if (i > 0) {
                        String appendix = Integer.toString(i + 1);
                        for (Map.Entry<String, String> entry : attrs.entrySet()) {
                            entity.put(entry.getKey() + appendix, entry.getValue());
                        }
                        entity.put(scoreAttr + appendix, features.getEntry(i*2+1));
                    }
                    else {
                        //TODO: change to use IOUtilities.parseEntityFromMap
                        for (Map.Entry<String, String> entry : attrs.entrySet()) {
                            entity.put(entry.getKey(), entry.getValue());
                        }
                        entity.put(scoreAttr, features.getEntry(i*2+1));
                    }
                }
            } else {
                logger.warn("{} does not exist in the {} to {} TFIDFKnnModel.", key, itemAttrs, feaAttrs);
            }
        }
        return initialResult;
    }
}
