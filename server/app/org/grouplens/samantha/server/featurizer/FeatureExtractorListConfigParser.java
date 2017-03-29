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

package org.grouplens.samantha.server.featurizer;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.ConfigurationException;
import play.Configuration;
import play.inject.Injector;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class FeatureExtractorListConfigParser implements FeaturizerConfigParser {
    private final Injector injector;

    @Inject
    public FeatureExtractorListConfigParser(Injector injector) {
        this.injector = injector;
    }

    public List<FeatureExtractorConfig> parse(Configuration featurizerConfig)
            throws ConfigurationException {
        List<Configuration> configList = featurizerConfig
                .getConfigList(ConfigKey.BASIC_FEATURE_EXTRACTOR_LIST_CONFIG.get());
        List<FeatureExtractorConfig> extConfigs = new ArrayList<>(configList.size());
        try {
            for (Configuration config : configList) {
                String extractorConfigClass = config
                        .getString(ConfigKey.FEATURE_EXTRACTOR_CONFIG_CLASS.get());
                Method method = Class.forName(extractorConfigClass)
                        .getMethod("getFeatureExtractorConfig", Configuration.class,
                                Injector.class);
                FeatureExtractorConfig configInstance = (FeatureExtractorConfig)(method
                        .invoke(null, config, injector));
                extConfigs.add(configInstance);
            }
        } catch (IllegalAccessException | InvocationTargetException |
                ClassNotFoundException | NoSuchMethodException e) {
            throw new ConfigurationException(e);
        }
        return extConfigs;
    }
}
