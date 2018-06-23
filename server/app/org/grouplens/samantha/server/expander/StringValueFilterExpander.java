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
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class StringValueFilterExpander implements EntityExpander {
    final private String filterAttr;
    final private boolean exclude;
    final private Set<String> values;
    final private boolean filterIfNotPresent;

    public StringValueFilterExpander(String filterAttr, List<String> values,
                                     boolean exclude, boolean filterIfNotPresent) {
        this.exclude = exclude;
        this.filterIfNotPresent = filterIfNotPresent;
        this.filterAttr = filterAttr;
        this.values = new HashSet<>(values);
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        boolean exclude = false;
        if (expanderConfig.asMap().containsKey("exclude")) {
            exclude = expanderConfig.getBoolean("exclude");
        }
        Boolean filterIfNotPresent = expanderConfig.getBoolean("filterIfNotPresent");
        if (filterIfNotPresent == null) {
            filterIfNotPresent = true;
        }
        return new StringValueFilterExpander(
                expanderConfig.getString("filterAttr"),
                expanderConfig.getStringList("values"),
                exclude, filterIfNotPresent);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            if (entity.has(filterAttr)) {
                String value = entity.get(filterAttr).asText();
                if ((values.contains(value) && !exclude) || (!values.contains(value) && exclude)) {
                    expanded.add(entity);
                }
            } else if (!filterIfNotPresent) {
                expanded.add(entity);
            }
        }
        return expanded;
    }
}
