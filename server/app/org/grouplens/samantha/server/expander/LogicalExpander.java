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
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class LogicalExpander implements EntityExpander {
    final private List<String> sourceAttrs;
    final private String valueAttr;
    final private boolean or;
    final private String separator;
    final private String joiner;

    public LogicalExpander(List<String> sourceAttrs, String valueAttr, String separator,
                            boolean or, String joiner) {
        this.sourceAttrs = sourceAttrs;
        this.valueAttr = valueAttr;
        this.separator = separator;
        this.or = or;
        this.joiner = joiner;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean or = expanderConfig.getBoolean("or");
        if (or == null) {
            or = false;
        }
        return new LogicalExpander(
                expanderConfig.getStringList("sourceAttrs"),
                expanderConfig.getString("valueAttr"),
                expanderConfig.getString("separator"), or,
                expanderConfig.getString("joiner"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            List<String[]> sources = new ArrayList<>();
            for (String nameAttr : sourceAttrs) {
                sources.add(entity.get(nameAttr).asText().split(separator, -1));
            }
            int size = sources.get(0).length;
            String[] values = new String[size];
            for (int i=0; i<size; i++) {
                values[i] = "1";
                if (or) {
                    values[i] = "0";
                }
                for (int j=0; j<sources.size(); j++) {
                    if ("".equals(sources.get(j))) {
                        values[i] = "";
                    } else {
                        double val = Double.parseDouble(sources.get(j)[i]);
                        if (val > 0.0 && or) {
                            values[i] = "1";
                            break;
                        } else if (val <= 0.0 && !or) {
                            values[i] = "0";
                            break;
                        }
                    }
                }
            }
            entity.put(valueAttr, StringUtils.join(values, joiner));
        }
        return initialResult;
    }
}
