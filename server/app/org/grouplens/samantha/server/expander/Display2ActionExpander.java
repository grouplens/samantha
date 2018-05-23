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

public class Display2ActionExpander implements EntityExpander {
    final private List<String> nameAttrs;
    final private List<String> valueAttrs;
    final private String actionName;
    final private String separator;
    final private String joiner;
    final private String tstampAttr;
    final private int splitTstamp;
    final private int maxGrpNum;
    final private int grpSize;
    final private String inGrpRank;
    final private boolean alwaysInclude;

    public Display2ActionExpander(List<String> nameAttrs, List<String> valueAttrs, String separator,
                                  String actionName, String joiner, String tstampAttr, int splitTstamp,
                                  int maxGrpNum, int grpSize, String inGrpRank, boolean alwaysInclude) {
        this.nameAttrs = nameAttrs;
        this.valueAttrs = valueAttrs;
        this.separator = separator;
        this.actionName = actionName;
        this.joiner = joiner;
        this.tstampAttr = tstampAttr;
        this.splitTstamp = splitTstamp;
        this.maxGrpNum = maxGrpNum;
        this.grpSize = grpSize;
        this.inGrpRank = inGrpRank;
        this.alwaysInclude = alwaysInclude;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Integer splitTstamp = expanderConfig.getInt("splitTstamp");
        if (splitTstamp == null) {
            splitTstamp = 0;
        }
        Integer maxGrpNum = expanderConfig.getInt("maxGrpNum");
        if (maxGrpNum == null) {
            maxGrpNum = Integer.MAX_VALUE;
        }
        Integer grpSize = expanderConfig.getInt("grpSize");
        if (grpSize == null) {
            grpSize = 1;
        }
        Boolean alwaysInclude = expanderConfig.getBoolean("alwaysInclude");
        if (alwaysInclude == null) {
            alwaysInclude = true;
        }
        return new Display2ActionExpander(
                expanderConfig.getStringList("nameAttrs"),
                expanderConfig.getStringList("valueAttrs"),
                expanderConfig.getString("separator"),
                expanderConfig.getString("actionName"),
                expanderConfig.getString("joiner"),
                expanderConfig.getString("tstampAttr"),
                splitTstamp, maxGrpNum, grpSize,
                expanderConfig.getString("inGrpRank"), alwaysInclude);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            List<String[]> values = new ArrayList<>();
            List<List<String>> valueStrs = new ArrayList<>();
            for (String nameAttr : nameAttrs) {
                values.add(entity.get(nameAttr).asText().split(separator, -1));
                valueStrs.add(new ArrayList<>());
            }
            String[] actions = entity.get(actionName).asText().split(separator, -1);
            int end = actions.length;
            if (tstampAttr != null) {
                String tstampStr = entity.get(tstampAttr).asText();
                if (!"".equals(tstampStr)) {
                    String[] fstamp = tstampStr.split(separator, -1);
                    int i;
                    for (i=0; i<fstamp.length; i++) {
                        String tstamp = fstamp[i];
                        int istamp = Integer.parseInt(tstamp);
                        if (istamp >= splitTstamp) {
                            break;
                        }
                    }
                    String[] indices = entity.get(inGrpRank).asText().split(separator, -1);
                    end = i + FeatureExtractorUtilities.getForwardEnd(
                            ArrayUtils.subarray(indices, i, end), maxGrpNum, grpSize);
                }
            }
            boolean include = false;
            for (int i=0; i<end; i++) {
                String act = actions[i];
                if (!"".equals(act) && Double.parseDouble(act) > 0.0) {
                    for (int j=0; j<valueStrs.size(); j++) {
                        valueStrs.get(j).add(values.get(j)[i]);
                    }
                    include = true;
                }
            }
            if (include || alwaysInclude) {
                ObjectNode newEntity = entity.deepCopy();
                for (int j=0; j<valueAttrs.size(); j++) {
                    newEntity.put(valueAttrs.get(j), StringUtils.join(valueStrs.get(j), joiner));
                }
                expanded.add(newEntity);
            }
        }
        return expanded;
    }
}
