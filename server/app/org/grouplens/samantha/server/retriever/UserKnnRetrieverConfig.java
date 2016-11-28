package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.modeler.knn.FeatureKnnModel;
import org.grouplens.samantha.modeler.knn.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class UserKnnRetrieverConfig implements RetrieverConfig {
    final private String retrieverName;
    final private String knnModelName;
    final private String kdnModelName;
    final private String knnModelFile;
    final private String kdnModelFile;
    final private String weightAttr;
    final private String scoreAttr;
    final private List<String> itemAttrs;
    final private List<String> userAttrs;
    final private int numNeighbors;
    final private int minSupport;
    final private String svdfeaPredictorName;
    final private String svdfeaModelName;
    final private Injector injector;
    final private List<Configuration> expandersConfig;
    final private Configuration config;

    private UserKnnRetrieverConfig(String retrieverName, String knnModelName, String kdnModelName,
                                   String knnModelFile, String kdnModelFile, int minSupport,
                                   String weightAttr, String scoreAttr, List<String> itemAttrs, List<String> userAttrs,
                                   int numNeighbors, String svdfeaPredictorName, String svdfeaModelName, Injector injector,
                                   List<Configuration> expandersConfig, Configuration config) {
        this.retrieverName = retrieverName;
        this.knnModelName = knnModelName;
        this.kdnModelName = kdnModelName;
        this.knnModelFile = knnModelFile;
        this.kdnModelFile = kdnModelFile;
        this.weightAttr = weightAttr;
        this.minSupport = minSupport;
        this.scoreAttr = scoreAttr;
        this.itemAttrs = itemAttrs;
        this.userAttrs = userAttrs;
        this.injector = injector;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.numNeighbors = numNeighbors;
        this.expandersConfig = expandersConfig;
        this.config = config;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        List<Configuration> expandersConfig = ExpanderUtilities.getEntityExpandersConfig(retrieverConfig);
        return new UserKnnRetrieverConfig(retrieverConfig.getString("userInterRetrieverName"),
                retrieverConfig.getString("knnModelName"),
                retrieverConfig.getString("kdnModelName"),
                retrieverConfig.getString("knnModelFile"),
                retrieverConfig.getString("kdnModelFile"),
                retrieverConfig.getInt("minSupport"),
                retrieverConfig.getString("weightAttr"),
                retrieverConfig.getString("scoreAttr"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getStringList("userAttrs"),
                retrieverConfig.getInt("numNeighbors"),
                retrieverConfig.getString("svdfeaPredictorName"),
                retrieverConfig.getString("svdfeaModelName"),
                injector, expandersConfig, retrieverConfig);
    }


    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelManager knnModelManager = new FeatureKnnModelManager(knnModelName, knnModelFile, injector,
                svdfeaPredictorName, svdfeaModelName, userAttrs, numNeighbors, false, minSupport);
        FeatureKnnModel knnModel = (FeatureKnnModel) knnModelManager.manage(requestContext);
        ModelManager kdnModelManager = new FeatureKnnModelManager(kdnModelName, kdnModelFile, injector,
                svdfeaPredictorName, svdfeaModelName, userAttrs, numNeighbors, true, minSupport);
        FeatureKnnModel kdnModel = (FeatureKnnModel) kdnModelManager.manage(requestContext);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        KnnModelFeatureTrigger trigger = new KnnModelFeatureTrigger(knnModel, kdnModel,
                userAttrs, weightAttr, scoreAttr);
        return new UserKnnRetriever(weightAttr, scoreAttr, userAttrs, itemAttrs, retriever, trigger, expanders, config);
    }
}
