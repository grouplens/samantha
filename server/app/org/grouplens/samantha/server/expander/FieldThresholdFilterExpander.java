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
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class FieldThresholdFilterExpander implements EntityExpander {
    private final String filterAttr;
    private final Double minVal;
    private final Double maxVal;
    private final boolean filterWhenNotPresent;

    public FieldThresholdFilterExpander(String filterAttr, Double minVal, Double maxVal,
                                        boolean filterWhenNotPresent) {
        this.filterAttr = filterAttr;
        this.minVal = minVal;
        this.maxVal = maxVal;
        this.filterWhenNotPresent = filterWhenNotPresent;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean filterWhenNotPresent = expanderConfig.getBoolean("filterWhenNotPresent");
        if (filterWhenNotPresent == null) {
            filterWhenNotPresent = true;
        }
        return new FieldThresholdFilterExpander(expanderConfig.getString("filterAttr"),
                expanderConfig.getDouble("minVal"),
                expanderConfig.getDouble("maxVal"),
                filterWhenNotPresent);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext){
        List<ObjectNode> filteredResult = new ArrayList<>();
        for (int i=0; i<initialResult.size(); i++) {
            ObjectNode entity = initialResult.get(i);
            if (entity.has(filterAttr)) {
                double val = entity.get(filterAttr).asDouble();
                if ((minVal != null && val < minVal) || (maxVal != null && val > maxVal)) {
                    continue;
                }
            } else {
                if (filterWhenNotPresent) {
                    continue;
                }
            }
            filteredResult.add(entity);
        }
        return filteredResult;
    }
}
