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
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.*;

public class LabelExpander implements EntityExpander {

    private final String labelAttr;
    private final Set<String> modeledLabels;
    private final boolean backward;

    public LabelExpander(String labelAttr, List<String> modeledLabels, boolean backward) {
        this.labelAttr = labelAttr;
        this.modeledLabels = new HashSet<>(modeledLabels);
        this.backward = backward;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new LabelExpander(
                expanderConfig.getString("labelAttr"),
                expanderConfig.getStringList("modeledLabels"),
                expanderConfig.getBoolean("backward"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        String userAttr = "userId";
        String itemAttr = "movieId";
        String tstampAttr = "tstamp";
        String[] historyAttrs = InactionUtilities.historyAttrs;
        Map<String, String[]> attr2seq = new HashMap<>();
        int splitTstamp = 1517464800; //2018-02-01
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            String user = entity.get(userAttr).asText();
            for (String attr : historyAttrs) {
                String[] seq = entity.get(attr).asText().split(",", -1);
                attr2seq.put(attr, seq);
            }
            String[] tstamps = attr2seq.get("tstamps");
            String[] labels = attr2seq.get(labelAttr + "s");
            String[] items = attr2seq.get("movieIds");
            for (int i=0; i<tstamps.length; i++) {
                if (modeledLabels.contains(labels[i])) {
                    int tstamp = Integer.parseInt(tstamps[i]);
                    if ((tstamp < splitTstamp && backward) || (tstamp >= splitTstamp && !backward)) {
                        ObjectNode features = InactionUtilities.getFeatures(attr2seq, i, labelAttr);
                        features.put(labelAttr, labels[i]);
                        features.put(userAttr, user);
                        features.put(itemAttr, items[i]);
                        features.put(tstampAttr, tstamp);
                        expanded.add(features);
                    }
                }
            }
        }
        return expanded;
    }
}
