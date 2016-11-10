package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.svdfeature.FeatureKnnModel;
import org.grouplens.samantha.modeler.svdfeature.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.common.*;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class RedisInteractionRetrieverConfig implements RetrieverConfig {
    final private String retrieverName;
    final private String knnModelName;
    final private String kdnModelName;
    final private String knnModelFile;
    final private String kdnModelFile;
    final private String weightAttr;
    final private String scoreAttr;
    final private List<String> itemAttrs;
    final private int numNeighbors;
    final private int minSupport;
    final private String predictorName;
    final private String predictorModelName;
    final private Injector injector;
    final private List<Configuration> expandersConfig;

    private RedisInteractionRetrieverConfig(String retrieverName, String knnModelName, String kdnModelName,
                                            String knnModelFile, String kdnModelFile, int minSupport,
                                            String weightAttr, String scoreAttr, List<String> itemAttrs, int numNeighbors,
                                            String predictorName, String predictorModelName, Injector injector,
                                            List<Configuration> expandersConfig) {
        this.retrieverName = retrieverName;
        this.knnModelName = knnModelName;
        this.kdnModelName = kdnModelName;
        this.knnModelFile = knnModelFile;
        this.kdnModelFile = kdnModelFile;
        this.weightAttr = weightAttr;
        this.minSupport = minSupport;
        this.scoreAttr = scoreAttr;
        this.itemAttrs = itemAttrs;
        this.injector = injector;
        this.predictorModelName = predictorModelName;
        this.predictorName = predictorName;
        this.numNeighbors = numNeighbors;
        this.expandersConfig = expandersConfig;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        List<Configuration> expandersConfig = ExpanderUtilities.getEntityExpandersConfig(retrieverConfig);
        return new RedisInteractionRetrieverConfig(retrieverConfig.getString("redisKeyBasedRetrieverName"),
                retrieverConfig.getString("knnModelName"),
                retrieverConfig.getString("kdnModelName"),
                retrieverConfig.getString("knnModelFile"),
                retrieverConfig.getString("kdnModelFile"),
                retrieverConfig.getInt("minSupport"),
                retrieverConfig.getString("weightAttr"),
                retrieverConfig.getString("scoreAttr"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getInt("numNeighbors"),
                retrieverConfig.getString("predictorName"),
                retrieverConfig.getString("predictorModelName"),
                injector, expandersConfig);
    }


    public Retriever getRetriever(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        String engineName = requestContext.getEngineName();
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);

        boolean toBuild = IOUtilities.whetherModelOperation(knnModelName, ModelOperation.BUILD, reqBody);
        FeatureKnnModel knnModel = RetrieverUtilities.getFeatureKnnModel(configService, modelService,
                requestContext, toBuild, knnModelName, predictorName, predictorModelName, itemAttrs,
                numNeighbors, false, minSupport, injector);
        boolean toLoad = IOUtilities.whetherModelOperation(knnModelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            knnModel = (FeatureKnnModel)RetrieverUtilities.loadModel(modelService, engineName, knnModelName,
                    knnModelFile);
        }
        boolean toDump = IOUtilities.whetherModelOperation(knnModelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(knnModel, knnModelFile);
        }

        toBuild = IOUtilities.whetherModelOperation(kdnModelName, ModelOperation.BUILD, reqBody);
        FeatureKnnModel kdnModel = RetrieverUtilities.getFeatureKnnModel(configService, modelService,
                requestContext, toBuild, kdnModelName, predictorName, predictorModelName, itemAttrs,
                numNeighbors, true, minSupport, injector);
        toLoad = IOUtilities.whetherModelOperation(kdnModelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            kdnModel = (FeatureKnnModel) RetrieverUtilities.loadModel(modelService, engineName, kdnModelName,
                    kdnModelFile);
        }
        toDump = IOUtilities.whetherModelOperation(kdnModelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(kdnModel, kdnModelFile);
        }

        RedisKeyBasedRetriever retriever = (RedisKeyBasedRetriever) configService.getRetriever(retrieverName,
                requestContext);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        KnnModelFeatureTrigger trigger = new KnnModelFeatureTrigger(knnModel, kdnModel,
                itemAttrs, weightAttr, scoreAttr);
        return new RedisInteractionRetriever(retriever, trigger, expanders);
    }
}
