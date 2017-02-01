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

import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class FeatureSupportRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    final private String modelName;
    final private String predictorName;
    final private List<String> itemAttrs;
    final private String supportAttr;
    final private Integer maxHits;
    final private Injector injector;

    private FeatureSupportRetrieverConfig(String modelName, String predictorName, List<String> itemAttrs,
                                          String supportAttr, Integer maxHits,
                                          Injector injector, Configuration config) {
        super(config);
        this.modelName = modelName;
        this.predictorName = predictorName;
        this.itemAttrs = itemAttrs;
        this.supportAttr = supportAttr;
        this.maxHits = maxHits;
        this.injector = injector;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new FeatureSupportRetrieverConfig(retrieverConfig.getString("modelName"),
                retrieverConfig.getString("predictorName"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getString("supportAttr"),
                retrieverConfig.getInt("maxHits"),
                injector, retrieverConfig);
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
