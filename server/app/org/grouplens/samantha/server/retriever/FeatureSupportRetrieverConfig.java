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

package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.*;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class FeatureSupportRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    final private String svdfeaModelName;
    final private String svdfeaPredictorName;
    final private String modelName;
    final private String modelFile;
    final private List<String> itemAttrs;
    final private String supportAttr;
    final private Integer maxHits;
    final private Injector injector;

    private FeatureSupportRetrieverConfig(String svdfeaModelName, String svdfeaPredictorName,
                                          String modelName, String modelFile,
                                          List<String> itemAttrs,
                                          String supportAttr, Integer maxHits,
                                          Injector injector, Configuration config) {
        super(config);
        this.modelName = modelName;
        this.modelFile = modelFile;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.itemAttrs = itemAttrs;
        this.supportAttr = supportAttr;
        this.maxHits = maxHits;
        this.injector = injector;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new FeatureSupportRetrieverConfig(
                retrieverConfig.getString("svdfeaModelName"),
                retrieverConfig.getString("svdfeaPredictorName"),
                retrieverConfig.getString("modelName"),
                retrieverConfig.getString("modelFile"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getString("supportAttr"),
                retrieverConfig.getInt("maxHits"),
                injector, retrieverConfig);
    }

    private class FeatureSupportModelManager extends AbstractModelManager {

        public FeatureSupportModelManager(String modelName, String modelFile, Injector injector) {
            super(injector, modelName, modelFile, new ArrayList<>());
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            return buildModel(null, requestContext);
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
            configService.getPredictor(svdfeaPredictorName, requestContext);
            ModelService modelService = injector.instanceOf(ModelService.class);
            SVDFeature svdfeaModel = (SVDFeature) modelService.getModel(requestContext.getEngineName(), svdfeaModelName);
            Object2DoubleMap<String> fea2sup = svdfeaModel.getFactorFeatures(10);
            List<Object2DoubleMap.Entry<String>> all = new ArrayList<>(fea2sup.size());
            for (Object2DoubleMap.Entry<String> entry : fea2sup.object2DoubleEntrySet()) {
                Map<String, String> keys = FeatureExtractorUtilities.decomposeKey(entry.getKey());
                boolean include = true;
                for (String attr : itemAttrs) {
                    if (!keys.containsKey(attr)) {
                        include = false;
                    }
                }
                if (include) {
                    all.add(entry);
                }
            }
            Ordering<Object2DoubleMap.Entry<String>> ordering = RetrieverUtilities.object2DoubleEntryOrdering();
            int limit = all.size();
            if (maxHits != null && maxHits < limit) {
                limit = maxHits;
            }
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, SpaceMode.BUILDING);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, SpaceMode.BUILDING);
            IndexedVectorModel vectorModel = new IndexedVectorModel(modelName, maxHits, 1, indexSpace, variableSpace);
            List<Object2DoubleMap.Entry<String>> results = ordering.greatestOf(all, limit);
            for (Object2DoubleMap.Entry<String> entry : results) {
                int index = vectorModel.ensureKey(entry.getKey());
                RealVector vector = MatrixUtils.createRealVector(new double[1]);
                vector.setEntry(0, entry.getDoubleValue());
                vectorModel.setIndexVector(index, vector);
            }
            return vectorModel;
        }
    }

    public Retriever getRetriever(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        FeatureSupportModelManager manager = new FeatureSupportModelManager(modelName, modelFile, injector);
        IndexedVectorModel model = (IndexedVectorModel) manager.manage(requestContext);
        List<ObjectNode> results = new ArrayList<>();
        int size = model.getIndexSize();
        for (int i=0; i<size; i++) {
            ObjectNode one = Json.newObject();
            Map<String, String> keys = FeatureExtractorUtilities.decomposeKey(model.getKeyByIndex(i));
            RealVector vector = model.getIndexVector(i);
            one.put(supportAttr, vector.getEntry(0));
            for (String attr : itemAttrs) {
                one.put(attr, keys.get(attr));
            }
            results.add(one);
        }
        return new PrecomputedRetriever(results, entityExpanders, config);
    }
}
