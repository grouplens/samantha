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

package org.grouplens.samantha.server.predictor;

import it.unimi.dsi.fastutil.objects.Object2DoubleMap;
import it.unimi.dsi.fastutil.objects.Object2DoubleOpenHashMap;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class FieldBlendingPredictorConfig implements PredictorConfig {
    private final Object2DoubleMap<String> defaults;
    private final Injector injector;
    private final Configuration daoConfigs;
    private final Configuration config;
    private final List<Configuration> expandersConfig;
    private final String daoConfigKey;

    private FieldBlendingPredictorConfig(Configuration config, Configuration daoConfigs, Injector injector,
                                         List<Configuration> expandersConfig,
                                         Object2DoubleMap<String> defaults,
                                         String daoConfigKey) {
        this.config = config;
        this.daoConfigs = daoConfigs;
        this.defaults = defaults;
        this.expandersConfig = expandersConfig;
        this.injector = injector;
        this.daoConfigKey = daoConfigKey;
    }

    public static PredictorConfig getPredictorConfig(Configuration predictorConfig,
                                                     Injector injector) {
        Object2DoubleMap<String> defaults = new Object2DoubleOpenHashMap<>();
        Configuration defaultConfig = predictorConfig.getConfig("blendingDefaults");
        for (String key : defaultConfig.keys()) {
            defaults.put(key, defaultConfig.getDouble(key));
        }
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(predictorConfig);
        return new FieldBlendingPredictorConfig(predictorConfig,
                predictorConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get()),
                injector, expanders, defaults, predictorConfig.getString("daoConfigKey"));
    }

    public Predictor getPredictor(RequestContext requestContext) {
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new FieldBlendingPredictor(config, daoConfigs, injector, entityExpanders, defaults, daoConfigKey);
    }
}
