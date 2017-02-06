package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Ordering;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.*;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
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
        private final List<EntityExpander> expanders;

        public FeatureSupportModelManager(String modelName, String modelFile, Injector injector,
                                          List<EntityExpander> expanders) {
            super(injector, modelName, modelFile, new ArrayList<>());
            this.expanders = expanders;
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            if (SpaceMode.BUILDING.equals(spaceMode)) {
                IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, SpaceMode.BUILDING);
                return indexSpace;
            } else {
                IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, SpaceMode.DEFAULT);
                return buildModel(indexSpace, requestContext);
            }
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
            configService.getPredictor(svdfeaPredictorName, requestContext);
            ModelService modelService = injector.instanceOf(ModelService.class);
            SVDFeature svdfeaModel = (SVDFeature) modelService.getModel(requestContext.getEngineName(),
                    svdfeaModelName);
            Object2DoubleMap<String> fea2sup = svdfeaModel.getFactorFeatures(10);
            List<ObjectNode> all = new ArrayList<>(fea2sup.size());
            for (Object2DoubleMap.Entry<String> entry : fea2sup.object2DoubleEntrySet()) {
                Map<String, String> keys = FeatureExtractorUtilities.decomposeKey(entry.getKey());
                ObjectNode one = Json.newObject();
                boolean include = true;
                for (String attr : itemAttrs) {
                    if (!keys.containsKey(attr)) {
                        include = false;
                    } else {
                        one.put(attr, keys.get(attr));
                    }
                }
                if (include) {
                    one.put(supportAttr, entry.getDoubleValue());
                    all.add(one);
                }
            }
            Ordering<ObjectNode> ordering = RetrieverUtilities.jsonFieldOrdering(supportAttr);
            int limit = all.size();
            if (maxHits != null && maxHits < limit) {
                limit = maxHits;
            }
            List<ObjectNode> results = ordering.greatestOf(all, limit);
            results = ExpanderUtilities.expand(results, expanders, requestContext);
            IndexSpace indexSpace = (IndexSpace) model;
            for (ObjectNode entry : results) {
                indexSpace.setKey(modelName, entry.toString());
            }
            return indexSpace;
        }
    }

    public Retriever getRetriever(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        FeatureSupportModelManager manager = new FeatureSupportModelManager(modelName, modelFile,
                injector, entityExpanders);
        IndexSpace model = (IndexSpace) manager.manage(requestContext);
        List<ObjectNode> results = new ArrayList<>();
        int size = model.getKeyMapSize(modelName);
        for (int i=0; i<size; i++) {
            ObjectNode one = Json.newObject();
            JsonNode stored = Json.parse((String)model.getKeyForIndex(modelName, i));
            IOUtilities.parseEntityFromJsonNode(stored, one);
            results.add(one);
        }
        return new PrecomputedRetriever(results, config);
    }
}
