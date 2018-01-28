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

public class SeparatedStringExpander implements EntityExpander {
    final private String nameAttr;
    final private String valueAttr;
    final private String separator;

    private SeparatedStringExpander(String nameAttr, String valueAttr, String separator) {
        this.nameAttr = nameAttr;
        this.valueAttr = valueAttr;
        this.separator = separator;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new SeparatedStringExpander(expanderConfig.getString("nameAttr"),
            expanderConfig.getString("valueAttr"), 
            expanderConfig.getString("separator"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<ObjectNode> oneExpanded = new ArrayList<>();
            String[] values = entity.get(nameAttr).asText().split(separator, -1);
            for (String value : values) {
                ObjectNode newEntity = entity.deepCopy();
                newEntity.put(valueAttr, value);
                oneExpanded.add(newEntity);
            }
            if (oneExpanded.size() > 0) {
                expanded.addAll(oneExpanded);
            } else {
                expanded.add(entity);
            }
        }
        return expanded;
    }
}
