package org.grouplens.samantha.ephemeral;

import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.ranker.RankerConfig;
import play.Configuration;
import play.inject.Injector;

public class EphemeralRankerConfig implements RankerConfig {
    private final Injector injector;
    private final String svdfeaturePredictor;
    private final String svdfeatureModel;
    private final Configuration config;

    public Ranker getRanker(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        configService.getPredictor(svdfeaturePredictor, requestContext);
        SVDFeature svdFeature = (SVDFeature) modelService.getModel(requestContext.getEngineName(), svdfeatureModel);
        return new EphemeralRanker(config, svdFeature);
    }

    private EphemeralRankerConfig(Configuration config, Injector injector, String svdfeaturePredictor,
                                  String svdfeatureModel) {
        this.injector = injector;
        this.config = config;
        this.svdfeatureModel = svdfeatureModel;
        this.svdfeaturePredictor = svdfeaturePredictor;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        return null;
    }
}
