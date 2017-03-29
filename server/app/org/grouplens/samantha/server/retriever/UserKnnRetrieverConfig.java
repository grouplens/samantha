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
import org.grouplens.samantha.modeler.knn.KnnModelFeatureTrigger;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.ModelManager;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class UserKnnRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
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

    private UserKnnRetrieverConfig(String retrieverName, String knnModelName, String kdnModelName,
                                   String knnModelFile, String kdnModelFile, int minSupport,
                                   String weightAttr, String scoreAttr, List<String> itemAttrs, List<String> userAttrs,
                                   int numNeighbors, String svdfeaPredictorName, String svdfeaModelName, Injector injector,
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
        this.userAttrs = userAttrs;
        this.injector = injector;
        this.svdfeaModelName = svdfeaModelName;
        this.svdfeaPredictorName = svdfeaPredictorName;
        this.numNeighbors = numNeighbors;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
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
                injector, retrieverConfig);
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
