package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class FeatureSupportRetrieverConfig implements RetrieverConfig {
    final private String modelName;
    final private String predictorName;
    final private List<String> itemAttrs;
    final private String supportAttr;
    final private Integer maxHits;
    final private List<Configuration> expandersConfig;
    final private Injector injector;
    final private Configuration config;

    private FeatureSupportRetrieverConfig(String modelName, String predictorName, List<String> itemAttrs,
                                          String supportAttr, Integer maxHits, List<Configuration> expandersConfig,
                                          Injector injector, Configuration config) {
        this.modelName = modelName;
        this.predictorName = predictorName;
        this.itemAttrs = itemAttrs;
        this.supportAttr = supportAttr;
        this.maxHits = maxHits;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.config = config;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(retrieverConfig);
        return new FeatureSupportRetrieverConfig(retrieverConfig.getString("modelName"),
                retrieverConfig.getString("predictorName"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getString("supportAttr"),
                retrieverConfig.getInt("maxHits"),
                expanders, injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        configService.getPredictor(predictorName, requestContext);
        ModelService modelService = injector.instanceOf(ModelService.class);
        SVDFeature model = (SVDFeature) modelService.getModel(requestContext.getEngineName(), modelName);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new FeatureSupportRetriever(model, itemAttrs, supportAttr, maxHits, entityExpanders, config);
    }
}
