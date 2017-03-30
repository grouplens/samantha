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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class LinearScaleExpander implements EntityExpander {
    private static Logger logger = LoggerFactory.getLogger(LinearScaleExpander.class);
    private final List<String> fieldNames;
    private final double minValue;
    private final double maxValue;
    private final double targetMinValue;
    private final double targetMaxValue;
    private final Boolean appendix;

    private LinearScaleExpander(List<String> fieldNames, double minValue, double maxValue, Boolean appendix,
                                double targetMinValue, double targetMaxValue) {
        this.fieldNames = fieldNames;
        this.maxValue = maxValue;
        this.minValue = minValue;
        this.appendix = appendix;
        this.targetMaxValue = targetMaxValue;
        this.targetMinValue = targetMinValue;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new LinearScaleExpander(expanderConfig.getStringList("fieldNames"),
                expanderConfig.getDouble("minValue"),
                expanderConfig.getDouble("maxValue"),
                expanderConfig.getBoolean("appendix"),
                expanderConfig.getDouble("targetMinValue"),
                expanderConfig.getDouble("targetMaxValue"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            for (String fieldName : fieldNames) {
                if (entity.has(fieldName)) {
                    double value = entity.get(fieldName).asDouble();
                    if (value >= maxValue) {
                        value = targetMaxValue;
                    } else if (value <= minValue) {
                        value = targetMinValue;
                    } else {
                        value = (value - minValue) * (targetMaxValue - targetMinValue) / (maxValue - minValue);
                    }
                    if (appendix) {
                        entity.put(fieldName + appendix, value);
                    } else {
                        entity.put(fieldName, value);
                    }
                } else {
                    logger.warn("The field {} is not present: {}", fieldName, entity.toString());
                }
            }
        }
        return initialResult;
    }
}
