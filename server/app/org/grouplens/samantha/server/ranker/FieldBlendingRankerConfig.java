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

package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

public class FieldBlendingRankerConfig implements RankerConfig {
    private final int pageSize;
    private final Injector injector;
    private final Object2DoubleMap<String> defaults;
    private final Configuration config;

    private FieldBlendingRankerConfig(int pageSize, Object2DoubleMap<String> defaults,
                                      Injector injector,
                                      Configuration config) {
        this.pageSize = pageSize;
        this.defaults = defaults;
        this.injector = injector;
        this.config = config;
    }

    public static RankerConfig getRankerConfig(Configuration rankerConfig,
                                               Injector injector) {
        int pageSize = 24;
        if (rankerConfig.asMap().containsKey(ConfigKey.RANKER_PAGE_SIZE.get())) {
            rankerConfig.getInt(ConfigKey.RANKER_PAGE_SIZE.get());
        }
        Object2DoubleMap<String> defaults = new Object2DoubleOpenHashMap<>();
        Configuration defaultConfig = rankerConfig.getConfig("blendingDefaults");
        for (String key : defaultConfig.keys()) {
            defaults.put(key, defaultConfig.getDouble(key));
        }
        return new FieldBlendingRankerConfig(pageSize, defaults, injector, rankerConfig);
    }

    public Ranker getRanker(RequestContext requestContext) {
        JsonNode requestBody = requestContext.getRequestBody();
        int page = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_PAGE.get(), 1);
        int offset = JsonHelpers.getOptionalInt(requestBody,
                ConfigKey.RANKER_OFFSET.get(), (page - 1) * pageSize);
        int limit = JsonHelpers.getOptionalInt(requestBody, ConfigKey.RANKER_LIMIT.get(), pageSize);
        return new FieldBlendingRanker(defaults, offset, limit, pageSize, requestContext, injector, config);
    }
}
