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

package org.grouplens.samantha.server.inaction;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.*;

public class LabelExpander implements EntityExpander {

    private final String labelAttr;
    private final Set<String> excludedLabels;
    private final int splitTstamp;
    private final boolean backward;

    public LabelExpander(String labelAttr, List<String> excludedLabels, int splitTstamp, boolean backward) {
        this.labelAttr = labelAttr;
        this.splitTstamp = splitTstamp;
        if (excludedLabels != null) {
            this.excludedLabels = new HashSet<>(excludedLabels);
        } else {
            this.excludedLabels = new HashSet<>();
        }
        this.backward = backward;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Integer splitTstamp = expanderConfig.getInt("splitTstamp");
        if (splitTstamp == null) {
            splitTstamp = 0;
        }
        return new LabelExpander(
                expanderConfig.getString("labelAttr"),
                expanderConfig.getStringList("excludedLabels"),
                splitTstamp,
                expanderConfig.getBoolean("backward"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        String userAttr = "userId";
        String itemAttr = "movieId";
        String tstampAttr = "tstamp";
        String[] historyAttrs = InactionUtilities.historyAttrs;
        Map<String, String[]> attr2seq = new HashMap<>();
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            String user = entity.get(userAttr).asText();
            for (String attr : historyAttrs) {
                String raw = attr.substring(0, attr.length() - 1);
                String[] seq = entity.get(raw).asText().split(",", -1);
                attr2seq.put(attr, seq);
            }
            String[] tstamps = attr2seq.get(tstampAttr + "s");
            String[] labels = attr2seq.get(labelAttr + "s");
            String[] items = attr2seq.get(itemAttr + "s");
            for (int i=0; i<tstamps.length; i++) {
                if (!excludedLabels.contains(labels[i])) {
                    int tstamp = Integer.parseInt(tstamps[i]);
                    if ((tstamp < splitTstamp && backward) || (tstamp >= splitTstamp && !backward)) {
                        ObjectNode features = InactionUtilities.getFeatures(attr2seq, i, labelAttr);
                        features.put(labelAttr, labels[i]);
                        features.put(userAttr, user);
                        features.put(itemAttr, items[i]);
                        features.put(tstampAttr, tstamp);
                        expanded.add(features);
                    } else {
                        throw new BadRequestException("wrong");
                    }
                }
            }
        }
        return expanded;
    }
}
