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
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class GroupTruncatingExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private String separator;
    final private String joiner;
    final private int maxGrpNum;
    final private int grpSize;
    final private String inGrpRank;
    final private boolean backward;

    public GroupTruncatingExpander(List<String> nameAttrs, List<String> valueAttrs,
                                   String separator, String joiner,
                                   int maxGrpNum, int grpSize, String inGrpRank, boolean backward) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.joiner = joiner;
        this.separator = separator;
        this.maxGrpNum = maxGrpNum;
        this.grpSize = grpSize;
        this.inGrpRank = inGrpRank;
        this.backward = backward;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Boolean backward = expanderConfig.getBoolean("backward");
        if (backward == null) {
            backward = false;
        }
        return new GroupTruncatingExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("joiner"),
                expanderConfig.getInt("maxGrpNum"),
                expanderConfig.getInt("grpSize"),
                expanderConfig.getString("inGrpRank"), backward);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            List<String[]> values = new ArrayList<>();
            for (String nameAttr : nameAttrs) {
                String[] splitted = entity.get(nameAttr).asText().split(separator, -1);
                values.add(splitted);
            }
            String[] indices = entity.get(inGrpRank).asText().split(separator, -1);
            int start = 0;
            int end = indices.length;
            if (backward) {
                Map.Entry<Integer, Integer> entry = FeatureExtractorUtilities.getStartAndNumGroup(
                        indices, maxGrpNum, grpSize);
                start = entry.getKey();
            } else {
                end = FeatureExtractorUtilities.getForwardEnd(indices, maxGrpNum, grpSize);
            }
            for (int j=0; j<values.size(); j++) {
                entity.put(valueAttrs.get(j), StringUtils.join(
                        ArrayUtils.subarray(values.get(j), start, end), joiner));
            }
        }
        return initialResult;
    }
}
