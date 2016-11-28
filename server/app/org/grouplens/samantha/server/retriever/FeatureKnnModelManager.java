package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.modeler.knn.FeatureKnnModel;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.inject.Injector;

import java.util.List;

public class FeatureKnnModelManager extends AbstractModelManager {
    private final String svdfeaPredictorName;
    private final String svdfeaModelName;
    private final List<String> itemAttrs;
    private final int numNeighbors;
    private final boolean reverse;
    private final int minSupport;

    public FeatureKnnModelManager(String modelName, String modelFile, Injector injector,
                                  String svdfeaPredictorName,
                                  String svdfeaModelName, List<String> itemAttrs,
                                  int numNeighbors, boolean reverse, int minSupport) {
        super(injector, modelName, modelFile);
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.itemAttrs = itemAttrs;
        this.numNeighbors = numNeighbors;
        this.reverse = reverse;
        this.minSupport = minSupport;
    }

    public Object createModel(RequestContext requestContext) {
        String engineName = requestContext.getEngineName();
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        configService.getPredictor(svdfeaPredictorName, requestContext);
        ModelService modelService = injector.instanceOf(ModelService.class);
        SVDFeature svdFeature = (SVDFeature) modelService.getModel(engineName,
                svdfeaModelName);
        SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
        FeatureKnnModel knnModel = new FeatureKnnModel(modelName, itemAttrs,
                numNeighbors, reverse, minSupport, svdFeature, indexSpace, variableSpace);
        return knnModel;
    }

    public Object buildModel(Object model, RequestContext requestContext) {
        FeatureKnnModel knnModel = (FeatureKnnModel) model;
        knnModel.buildModel();
        return model;
    }
}
