/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

package org.grouplens.samantha.server.xgboost;

import org.grouplens.samantha.modeler.xgboost.XGBoostModel;
import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.Retriever;
import org.grouplens.samantha.server.retriever.RetrieverConfig;
import play.Configuration;
import play.inject.Injector;

public class XGBoostClassifierRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    private final Injector injector;
    private final String predictorName;
    private final String modelName;

    private XGBoostClassifierRetrieverConfig(String predictorName, String modelName,
                                             Injector injector, Configuration config) {
        super(config);
        this.injector = injector;
        this.modelName = modelName;
        this.predictorName = predictorName;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new XGBoostClassifierRetrieverConfig(
                retrieverConfig.getString("predictorName"),
                retrieverConfig.getString("modelName"),
                injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ModelService modelService = injector.instanceOf(ModelService.class);
        configService.getPredictor(predictorName, requestContext);
        XGBoostModel model = (XGBoostModel) modelService.getModel(requestContext.getEngineName(), modelName);
        return new XGBoostClassifierRetriever(model, config, requestContext, injector);
    }
}
