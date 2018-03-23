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
import com.google.common.collect.Lists;
import play.libs.Json;

import java.util.AbstractMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class InactionUtilities {

    static public final String[] historyAttrs = {
            "notices",
            "tstamps", "sessionIds", "pageNames", "pageSizes", "dwells", "movieIds", "ranks",
            "clicks", "ratings", "highRates", "lowRates", "trailers", "wishlists", "hovers", "stops",
            "positives", "negatives", "actions",
            "reasons", "familiars", "whens", "rates", "futures", "skips",
    };
    static public final String[] acts = {
            "action", "positive", "negative",
            "wishlist", "rating", "highRate", "lowRate",
            "click", "stop", "hover"};

    static private Map.Entry<Integer, Integer> getPageRange(int index, String[] ranks) {
        int begin = index;
        int end = index + 1;
        int cur = Integer.parseInt(ranks[index]);

        int prev = cur;
        while (end < ranks.length) {
            int rank = Integer.parseInt(ranks[end]);
            if (rank > prev) {
                end++;
                prev = rank;
            } else {
                break;
            }
        }
        prev = cur;
        while (begin > 0) {
            int rank = Integer.parseInt(ranks[begin-1]);
            if (rank < prev) {
                begin--;
                prev = rank;
            } else {
                break;
            }
        }
        return new AbstractMap.SimpleEntry<>(begin, end);
    }

    static private double getInverseDistance(int row1, int col1, int row2, int col2) {
        double dist = Math.sqrt((row1 - row2) * (row1 - row2) + (col1 - col2) * (col1 - col2));
        return 1.0 / (dist + 1.0);
    }

    static private void extractPageInfo(ObjectNode features, Map<String, String[]> attr2seq,
                                        String appendix, int begin, int end, int index) {
        features.put("name" + appendix, "none");
        features.put("size" + appendix, 0);
        features.put("dwell" + appendix, 0.0);
        features.put("row" + appendix, -1);
        features.put("col" + appendix, -1);
        features.put("closestAction" + appendix, "none");
        features.put("closestInvDist" + appendix, -1);
        features.put("closestRow" + appendix, -1);
        features.put("closestCol" + appendix, -1);
        for (String act : acts) {
            features.put(act + appendix, 0);
        }
        if (index >= 0) {
            features.put("name" + appendix, attr2seq.get("pageNames")[index]);
            features.put("size" + appendix, Integer.parseInt(attr2seq.get("pageSizes")[index]));
            features.put("dwell" + appendix, Float.parseFloat(attr2seq.get("dwells")[index]));
            int rank = Integer.parseInt(attr2seq.get("ranks")[index]);
            features.put("row" + appendix, rank / 8);
            features.put("col" + appendix, rank % 8);
        }
        for (int i=begin; i<end; i++) {
            for (String act : acts) {
                if (Integer.parseInt(attr2seq.get(act + "s")[i]) > 0) {
                    features.put(act + appendix, features.get(act + appendix).asInt() + 1);
                    if (index >= 0) {
                        int rank = Integer.parseInt(attr2seq.get("ranks")[i]);
                        double invDist = getInverseDistance(
                                rank / 8, rank % 8,
                                features.get("row" + appendix).asInt(),
                                features.get("col" + appendix).asInt());
                        if (invDist >= features.get("closestInvDist" + appendix).asDouble()) {
                            features.put("closestAction" + appendix, act);
                            features.put("closestInvDist" + appendix, invDist);
                            features.put("closestRow" + appendix, rank / 8);
                            features.put("closestCol" + appendix, rank % 8);
                        }
                    }
                }
            }
        }
    }

    static private void extractUserLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {

    }

    static private void extractPageLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        Map.Entry<Integer, Integer> pageRange = getPageRange(index, attr2seq.get("ranks"));
        int begin = pageRange.getKey();
        int end = pageRange.getValue();
        extractPageInfo(features, attr2seq, "Page", begin, end, index);
    }

    static private void extractItemLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        features.put("hover", attr2seq.get("hovers")[index]);
    }

    static private void extractSurvey(ObjectNode features, Map<String, String[]> attr2seq,
                                      int index, String labelAttr) {
        // set inaction
        String inaction = "Unknown";
        String detailInaction = "Unknown";
        if (!attr2seq.get("notices")[index].equals("Noticed")) {
            inaction = "NotNoticed";
            detailInaction = "NotNoticed";
        } else if (attr2seq.get("familiars")[index].equals("Watched")) {
            inaction = "Watched";
            detailInaction = "Watched";
            String when = attr2seq.get("whens")[index];
            if (when.equals("PastWeek") || when.equals("PastMonth")) {
                detailInaction = "PastMonth";
            } else if (when.equals("Months6") || when.equals("Months12")) {
                detailInaction = "PastYear";
            } else if (when.equals("Years3") || when.equals("Years3More") || when.equals("DidNotRemember")) {
                detailInaction = "YearsAgo";
            }
        } else {
            String skip = attr2seq.get("skips")[index];
            String[] considered = {
                    "DecidedToWatch", "ExploreLater", "OthersBetter", "NotNow", "WouldNotEnjoy"};
            Set<String> set = new HashSet<>(Lists.newArrayList(considered));
            if (set.contains(skip)) {
                inaction = skip;
                detailInaction = skip;
            }
        }
        features.put("inaction", inaction);
        features.put("detailInaction", detailInaction);
        features.put("reason", attr2seq.get("reasons")[index]);
        if (!labelAttr.equals("familiar")) {
            features.put("familiar", attr2seq.get("familiars")[index]);
            features.put("when", attr2seq.get("whens")[index]);
        }
    }

    static public ObjectNode getFeatures(
            Map<String, String[]> attr2seq, int index, String labelAttr) {
        ObjectNode features = Json.newObject();
        extractUserLevel(features, attr2seq, index);
        extractPageLevel(features, attr2seq, index);
        extractItemLevel(features, attr2seq, index);
        extractSurvey(features, attr2seq, index, labelAttr);
        return features;
    }
}
