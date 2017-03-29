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

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AbstractPredictor implements Predictor {
    protected final Configuration config;
    protected final Configuration daoConfigs;
    protected final Injector injector;
    private final String daoConfigKey;
    private final boolean verbose;

    public AbstractPredictor(Configuration config, Configuration daoConfigs,
                             String daoConfigKey, Injector injector) {
        this.config = config;
        this.daoConfigKey = daoConfigKey;
        this.daoConfigs = daoConfigs;
        this.injector = injector;
        Boolean verboseConfig = config.getBoolean(ConfigKey.ENGINE_COMPONENT_VERBOSE.get());
        if (verboseConfig != null) {
            this.verbose = verboseConfig;
        } else {
            this.verbose = false;
        }
    }

    public List<Prediction> predict(RequestContext requestContext) {
        return PredictorUtilities.predictFromRequest(this, requestContext, injector, daoConfigs, daoConfigKey);
    }

    public Configuration getConfig() {
        if (verbose) {
            return config;
        } else {
            Map<String, Object> configMap = new HashMap<>();
            configMap.put(ConfigKey.ENGINE_COMPONENT_NAME.get(),
                    config.getString(ConfigKey.ENGINE_COMPONENT_NAME.get()));
            return new Configuration(configMap);
        }
    }
}
