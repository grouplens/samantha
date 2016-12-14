package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.modeler.knn.FeatureKnnModel;
import org.grouplens.samantha.modeler.knn.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.common.*;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class ItemKnnRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
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
    final private String svdfeaPredictorName;
    final private String svdfeaModelName;
    final private Injector injector;

    private ItemKnnRetrieverConfig(String retrieverName, String knnModelName, String kdnModelName,
                                   String knnModelFile, String kdnModelFile, int minSupport,
                                   String weightAttr, String scoreAttr, List<String> itemAttrs, int numNeighbors,
                                   String svdfeaPredictorName, String svdfeaModelName, Injector injector,
                                   Configuration config) {
        super(config);
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
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.numNeighbors = numNeighbors;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new ItemKnnRetrieverConfig(retrieverConfig.getString("userInterRetrieverName"),
                retrieverConfig.getString("knnModelName"),
                retrieverConfig.getString("kdnModelName"),
                retrieverConfig.getString("knnModelFile"),
                retrieverConfig.getString("kdnModelFile"),
                retrieverConfig.getInt("minSupport"),
                retrieverConfig.getString("weightAttr"),
                retrieverConfig.getString("scoreAttr"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getInt("numNeighbors"),
                retrieverConfig.getString("svdfeaPredictorName"),
                retrieverConfig.getString("svdfeaModelName"),
                injector, retrieverConfig);
    }


    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Retriever retriever = configService.getRetriever(retrieverName, requestContext);
        List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext, expandersConfig, injector);
        ModelManager knnModelManager = new FeatureKnnModelManager(knnModelName, knnModelFile, injector,
                svdfeaPredictorName, svdfeaModelName, itemAttrs, numNeighbors, false, minSupport);
        FeatureKnnModel knnModel = (FeatureKnnModel) knnModelManager.manage(requestContext);
        ModelManager kdnModelManager = new FeatureKnnModelManager(kdnModelName, kdnModelFile, injector,
                svdfeaPredictorName, svdfeaModelName, itemAttrs, numNeighbors, true, minSupport);
        FeatureKnnModel kdnModel = (FeatureKnnModel) kdnModelManager.manage(requestContext);
        KnnModelFeatureTrigger trigger = new KnnModelFeatureTrigger(knnModel, kdnModel,
                itemAttrs, weightAttr, scoreAttr);
        return new ItemKnnRetriever(retriever, trigger, expanders, config);
    }
}
