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

import org.grouplens.samantha.modeler.knn.FeatureKnnModel;
import org.grouplens.samantha.modeler.model.IndexSpace;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.modeler.model.SpaceProducer;
import org.grouplens.samantha.modeler.model.VariableSpace;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FeatureKnnModelManager extends AbstractModelManager {
    private final String svdfeaPredictorName;
    private final String svdfeaModelName;
    private final List<String> itemAttrs;
    private final int numNeighbors;
    private final boolean reverse;
    private final int minSupport;
    private final int numMatch;

    public FeatureKnnModelManager(String modelName, String modelFile, Injector injector,
                                  String svdfeaPredictorName, String svdfeaModelName,
                                  List<String> itemAttrs, int numMatch,
                                  int numNeighbors, boolean reverse, int minSupport) {
        super(injector, modelName, modelFile, new ArrayList<>());
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.itemAttrs = itemAttrs;
        this.numNeighbors = numNeighbors;
        this.reverse = reverse;
        this.minSupport = minSupport;
        this.numMatch = numMatch;
    }

    public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
        String engineName = requestContext.getEngineName();
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        configService.getPredictor(svdfeaPredictorName, requestContext);
        ModelService modelService = injector.instanceOf(ModelService.class);
        SVDFeature svdFeature = (SVDFeature) modelService.getModel(engineName,
                svdfeaModelName);
        SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
        FeatureKnnModel knnModel = new FeatureKnnModel(modelName, itemAttrs, numMatch,
                numNeighbors, reverse, minSupport, svdFeature, indexSpace, variableSpace);
        return knnModel;
    }

    public Object buildModel(Object model, RequestContext requestContext) {
        FeatureKnnModel knnModel = (FeatureKnnModel) model;
        knnModel.buildModel();
        return model;
    }
}
