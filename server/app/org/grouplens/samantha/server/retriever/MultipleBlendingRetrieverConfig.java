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

package org.grouplens.samantha.server.retriever;

import org.grouplens.samantha.server.common.AbstractComponentConfig;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class MultipleBlendingRetrieverConfig extends AbstractComponentConfig implements RetrieverConfig {
    private final List<String> retrieverNames;
    private final List<String> itemAttrs;
    private final Integer maxHits;
    private final Injector injector;

    private MultipleBlendingRetrieverConfig(List<String> retrieverNames, List<String> itemAttrs, Integer maxHits,
                                            Injector injector, Configuration config) {
        super(config);
        this.injector = injector;
        this.retrieverNames = retrieverNames;
        this.itemAttrs = itemAttrs;
        this.maxHits = maxHits;
    }

    public static RetrieverConfig getRetrieverConfig(Configuration retrieverConfig,
                                                     Injector injector) {
        return new MultipleBlendingRetrieverConfig(retrieverConfig.getStringList("retrieverNames"),
                retrieverConfig.getStringList("itemAttrs"),
                retrieverConfig.getInt("maxHits"), injector, retrieverConfig);
    }

    public Retriever getRetriever(RequestContext requestContext) {
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        List<Retriever> retrievers = new ArrayList<>(retrieverNames.size());
        for (String name : retrieverNames) {
            retrievers.add(configService.getRetriever(name, requestContext));
        }
        return new MultipleBlendingRetriever(retrievers, itemAttrs, maxHits, config, requestContext, injector);
    }
}
