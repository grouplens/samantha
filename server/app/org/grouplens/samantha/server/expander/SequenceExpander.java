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
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class SequenceExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private List<String> historyAttrs;
    final private String tstampAttr;
    final private int splitTstamp;
    final private String separator;
    final private String joiner;
    final private Integer maxStepNum;
    final private boolean backward;

    public SequenceExpander(List<String> nameAttrs, List<String> valueAttrs,
                            List<String> historyAttrs, String separator, String joiner,
                            Integer maxStepNum, boolean backward, String tstampAttr, int splitTstamp) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.historyAttrs = historyAttrs;
        this.joiner = joiner;
        this.separator = separator;
        this.maxStepNum = maxStepNum;
        this.backward = backward;
        this.splitTstamp = splitTstamp;
        this.tstampAttr = tstampAttr;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean backward = expanderConfig.getBoolean("backward");
        if (backward == null) {
            backward = false;
        }
        Integer splitTstamp = expanderConfig.getInt("splitTstamp");
        if (splitTstamp == null) {
            splitTstamp = 0;
        }
        return new SequenceExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getStringList("historyAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getInt("maxStepNum"), backward,
                expanderConfig.getString("tstampAttr"), splitTstamp);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<ObjectNode> oneExpanded = new ArrayList<>();
            List<String[]> values = new ArrayList<>();
            int size = 0;
            for (String nameAttr : nameAttrs) {
                String[] splitted = entity.get(nameAttr).asText().split(separator, -1);
                size = splitted.length;
                values.add(splitted);
            }
            int start = 0;
            int end = size;
            if (tstampAttr != null) {
                String[] fstamp = entity.get(tstampAttr).asText().split(separator, -1);
                int i;
                for (i=0; i<size; i++) {
                    String tstamp = fstamp[i];
                    int istamp = Integer.parseInt(tstamp);
                    if (istamp >= splitTstamp) {
                        break;
                    }
                }
                if (backward) {
                    end = i;
                } else {
                    start = i;
                }
                size = end - start;
            }
            if (maxStepNum != null && maxStepNum < size) {
                if (backward) {
                    start = end - maxStepNum;
                } else {
                    end = start + maxStepNum;
                }
            }
            for (int i=start; i<end; i++) {
                ObjectNode newEntity = entity.deepCopy();
                for (int j=0; j<values.size(); j++) {
                    newEntity.put(valueAttrs.get(j), values.get(j)[i]);
                    newEntity.put(historyAttrs.get(j), StringUtils.join(
                            ArrayUtils.subarray(values.get(j), 0, i), joiner));
                }
                oneExpanded.add(newEntity);
            }
            expanded.addAll(oneExpanded);
        }
        return expanded;
    }
}
