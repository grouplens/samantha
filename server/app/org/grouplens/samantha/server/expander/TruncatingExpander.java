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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class TruncatingExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private String separator;
    final private String joiner;
    final private int maxStepNum;
    final private boolean backward;

    public TruncatingExpander(List<String> nameAttrs, List<String> valueAttrs,
                              String separator, String joiner,
                              int maxStepNum, boolean backward) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.joiner = joiner;
        this.separator = separator;
        this.maxStepNum = maxStepNum;
        this.backward = backward;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean backward = expanderConfig.getBoolean("backward");
        if (backward == null) {
            backward = false;
        }
        return new TruncatingExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getInt("maxStepNum"), backward);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            List<String[]> values = new ArrayList<>();
            int size = 0;
            for (String nameAttr : nameAttrs) {
                String[] splitted = entity.get(nameAttr).asText().split(separator, -1);
                size = splitted.length;
                values.add(splitted);
            }
            int start = 0;
            int end = size;
            if (maxStepNum < size) {
                if (backward) {
                    start = end - maxStepNum;
                } else {
                    end = start + maxStepNum;
                }
            }
            for (int j=0; j<values.size(); j++) {
                entity.put(valueAttrs.get(j), StringUtils.join(
                        ArrayUtils.subarray(values.get(j), start, end), joiner));
            }
        }
        return initialResult;
    }
}
