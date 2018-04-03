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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import it.unimi.dsi.fastutil.doubles.DoubleList;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.svdfeature.SVDFeature;
import org.grouplens.samantha.modeler.tensorflow.TensorFlowModel;
import play.libs.Json;

import java.util.*;

public class InactionUtilities {

    static private double[][] weights = null;

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
            "click", "stop", "hover",
    };

    static public final String[] surs = {
            "reason", "notice", "familiar", "when", "rate", "skip", "future", "inaction", "detailInaction"
    };

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

    static private Map.Entry<Integer, Integer> getSessionRange(
            int index, String[] sessionIds, Map.Entry<Integer, Integer> pageRange) {
        String cur = sessionIds[index];
        while (index - 1 >= 0) {
            if (!sessionIds[index - 1].equals(cur)) {
                break;
            }
            index--;
        }
        return new AbstractMap.SimpleEntry<>(index, pageRange.getValue());
    }

    static private double getInverseDistance(int row1, int col1, int row2, int col2) {
        double dist = Math.sqrt((row1 - row2) * (row1 - row2) + (col1 - col2) * (col1 - col2));
        return 1.0 / (dist + 1.0);
    }

    static private void getRangeActionAndRate(
            ObjectNode features, Map<String, String[]> attr2seq,
            String appendix, int begin, int end) {
        for (String act : acts) {
            features.put(act + appendix, 0);
        }
        for (int i=begin; i<end; i++) {
            for (String act : acts) {
                if (Integer.parseInt(attr2seq.get(act + "s")[i]) > 0) {
                    features.put(act + appendix, features.get(act + appendix).asInt() + 1);
                }
            }
        }
        for (String act : acts) {
            features.put(act + "Rate" + appendix, -1);
            if (end > begin) {
                features.put(act + "Rate" + appendix, features.get(act + appendix).asDouble() / (end - begin));
            }
        }
    }

    static private int extractClosestActionInfo(
            ObjectNode features, Map<String, String[]> attr2seq,
            String appendix, int begin, int end, int index) {
        features.put("closestAction" + appendix, "none");
        features.put("closestInvDist" + appendix, -1);
        features.put("closestRow" + appendix, -1);
        features.put("closestCol" + appendix, -1);
        int closest = -1;
        if (index >= 0) {
            int targetRank = Integer.parseInt(attr2seq.get("ranks")[index]);
            for (int i = begin; i < end; i++) {
                for (String act : acts) {
                    if (Integer.parseInt(attr2seq.get(act + "s")[i]) > 0) {
                        int rank = Integer.parseInt(attr2seq.get("ranks")[i]);
                        double invDist = getInverseDistance(
                                rank / 8, rank % 8,
                                targetRank / 8, targetRank % 8);
                        if (invDist >= features.get("closestInvDist" + appendix).asDouble()) {
                            features.put("closestAction" + appendix, act);
                            features.put("closestInvDist" + appendix, invDist);
                            features.put("closestRow" + appendix, rank / 8);
                            features.put("closestCol" + appendix, rank % 8);
                            closest = i;
                        }
                    }
                }
            }
        }
        return closest;
    }

    static private void extractPageInfo(ObjectNode features, Map<String, String[]> attr2seq,
                                        String appendix, int index) {
        features.put("name" + appendix, "none");
        features.put("size" + appendix, 0);
        features.put("dwell" + appendix, 0.0);
        features.put("row" + appendix, -1);
        features.put("col" + appendix, -1);
        if (index >= 0) {
            features.put("name" + appendix, attr2seq.get("pageNames")[index]);
            features.put("size" + appendix, Integer.parseInt(attr2seq.get("pageSizes")[index]));
            features.put("dwell" + appendix, Float.parseFloat(attr2seq.get("dwells")[index]));
            int rank = Integer.parseInt(attr2seq.get("ranks")[index]);
            features.put("row" + appendix, rank / 8);
            features.put("col" + appendix, rank % 8);
        }
    }

    static private IntList searchHitsInRange(String[] items, int index, int begin, int end) {
        IntList hits = new IntArrayList();
        String item = items[index];
        for (int i=begin; i<end; i++) {
            if (items[i].equals(item)) {
                hits.add(i);
            }
        }
        return hits;
    }

    static private void extractUserLevel(
            ObjectNode features, Map<String, String[]> attr2seq, int index,
            Map.Entry<Integer, Integer> pageRange,
            Map.Entry<Integer, Integer> sessRange) {
        getRangeActivity(features, attr2seq, 0, sessRange.getValue(), "Hist");
        getRangeActionAndRate(features, attr2seq, "Hist", 0, sessRange.getValue());
        getRangeHitsInfo(features, attr2seq, "Hist", index, 0, index);
        getRangeHitsInfo(features, attr2seq, "OutSess", index, 0, sessRange.getKey());
        getInverseLastPageReview(features, attr2seq, 0, sessRange.getValue(),
                pageRange.getKey(), pageRange.getValue(), "Hist");
        getInverseLastPageReview(features, attr2seq, 0, sessRange.getKey(),
                pageRange.getKey(), pageRange.getValue(), "OutSess");
    }

    static private void getRangeActivity(
            ObjectNode features, Map<String, String[]> attr2seq, int begin, int end, String appendix) {
        int beginTstamp = Integer.parseInt(attr2seq.get("tstamps")[begin]);
        int endTstamp = Integer.parseInt(attr2seq.get("tstamps")[end - 1]);
        int length = endTstamp - beginTstamp;
        int numItem = end - begin;
        double meanNumItem = -1.0;
        if (length > 0) {
            meanNumItem = numItem * 1.0 / length;
        }
        features.put("length" + appendix, length);
        features.put("numItem" + appendix, numItem);
        features.put("meanNumItem" + appendix, meanNumItem);
    }

    static private void getRangeHitsInfo(
            ObjectNode features, Map<String, String[]> attr2seq,  String appendix,
            int index, int begin, int end) {
        IntList hits = searchHitsInRange(attr2seq.get("movieIds"), index, begin, end);
        features.put("numShow" + appendix, hits.size());
        int numExploreShow = 0;
        int numFrontShow = 0;
        for (int i : hits) {
            if (attr2seq.get("pageNames")[i].equals("base.explore")) {
                numExploreShow++;
            } else {
                numFrontShow++;
            }
        }
        features.put("numExploreShow" + appendix, numExploreShow);
        features.put("numFrontShow" + appendix, numFrontShow);
        int lastHit = -1;
        if (hits.size() > 1) {
            lastHit = hits.get(hits.size() - 1);
        }
        extractPageInfo(features, attr2seq, "PrevShowSess", lastHit);
    }

    static private boolean checkSamePage(String[] items1, String[] items2) {
        if (items1.length != items2.length) {
            return false;
        }
        for (int i=0; i<items1.length; i++) {
            if (!items1[i].equals(items2[i])) {
                return false;
            }
        }
        return true;
    }

    static private void getInverseLastPageReview(
            ObjectNode features, Map<String, String[]> attr2seq,
            int rangeBegin, int rangeEnd, int pageBegin, int pageEnd, String appendix) {
        String[] items = attr2seq.get("movieIds");
        String[] ranks = attr2seq.get("ranks");
        String[] pageItems = ArrayUtils.subarray(items, pageBegin, pageEnd);
        int cur = rangeEnd - 1;
        int cnt = 0;
        int dist = 0;
        while (cur >= rangeBegin) {
            Map.Entry<Integer, Integer> pageRange = getPageRange(cur, ranks);
            if (checkSamePage(pageItems, ArrayUtils.subarray(items, pageRange.getKey(), pageRange.getValue()))) {
                cnt += 1;
            }
            if (cnt <= 1) {
                dist++;
            }
            cur = pageRange.getKey() - 1;
        }
        if (dist == 0) {
            features.put("invReviewDist" + appendix, 0.0);
        } else {
            features.put("invReviewDist" + appendix, 1.0 / dist);
        }
        features.put("numReview" + appendix, cnt);
    }

    static private Map.Entry<Integer, Integer> extractSessionLevel(
            ObjectNode features, Map<String, String[]> attr2seq, int index,
            Map.Entry<Integer, Integer> pageRange) {
        Map.Entry<Integer, Integer> sessRange = getSessionRange(index, attr2seq.get("sessionIds"), pageRange);
        getRangeActivity(features, attr2seq, sessRange.getKey(), sessRange.getValue(), "Sess");
        getRangeActionAndRate(features, attr2seq, "Sess", sessRange.getKey(), sessRange.getValue());
        extractPageInfo(features, attr2seq, "Prev", pageRange.getKey() - 1);
        getRangeHitsInfo(features, attr2seq, "Sess", index, sessRange.getKey(), index);
        getInverseLastPageReview(features, attr2seq, sessRange.getKey(), sessRange.getValue(),
                pageRange.getKey(), pageRange.getValue(), "Sess");
        return sessRange;
    }

    static private Map.Entry<Integer, Integer> extractPageLevel(
            ObjectNode features, Map<String, String[]> attr2seq, int index) {
        String[] ranks = attr2seq.get("ranks");
        Map.Entry<Integer, Integer> pageRange = getPageRange(index, ranks);
        int begin = pageRange.getKey();
        int end = pageRange.getValue();
        extractPageInfo(features, attr2seq, "Page", index);
        getRangeActionAndRate(features, attr2seq, "Page", begin, end);
        return pageRange;
    }

    static private void extractItemLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        features.put("hoverItem", attr2seq.get("hovers")[index]);
        features.put("actionItem", attr2seq.get("actions")[index]);
    }

    static public void getInaction(Map<String, String[]> attr2seq) {
        String[] reasons = attr2seq.get("reasons");
        String[] inactions = new String[reasons.length];
        String[] detailInactions = new String[reasons.length];
        for (int index=0; index<reasons.length; index++) {
            String inaction = "NoPop";
            String detailInaction = "NoPop";
            if (attr2seq.get("notices")[index].equals("DidNotNotice") ||
                    attr2seq.get("notices")[index].equals("NotDisplayed")) {
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
            inactions[index] = inaction;
            detailInactions[index] = detailInaction;
        }
        attr2seq.put("inactions", inactions);
        attr2seq.put("detailInactions", detailInactions);
    }

    static public void extractSurvey(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        for (String sur : surs) {
            features.put(sur + "Sur", attr2seq.get(sur + "s")[index]);
        }
    }

    static public void extractItemInfoFeatures(ObjectNode features, Map<String, String[]> attr2seq, int index,
                                               List<JsonNode> item2info) {
        String[] items = attr2seq.get("movieIds");
        int itemId = Integer.parseInt(items[index]);
        JsonNode info = item2info.get(itemId);
        if (info != null) {
            features.put("popularityItem", info.get("popularity").asInt());
            int release = info.get("releaseYear").asInt();
            if (release >= 1907) {
                features.put("releaseYearItem", release);
            } else {
                features.put("releaseYearItem", 1990);
            }
        } else {
            features.put("popularityItem", 167);
            features.put("releaseYearItem", 1990);
        }
    }

    static public ObjectNode constructSequence(Map<String, String[]> attr2seq, int endIndex,
                                               String userAttr, String userId) {
        ObjectNode entity = Json.newObject();
        entity.put(userAttr, userId);
        for (String attr : historyAttrs) {
            if (attr2seq.containsKey(attr)) {
                entity.put(attr.substring(0, attr.length() - 1),
                        StringUtils.join(ArrayUtils.subarray(attr2seq.get(attr), 0, endIndex), ","));
            }
        }
        return entity;
    }

    static private void extractPredictedFeatures(ObjectNode features, TensorFlowModel model,
                                                 ObjectNode entity, String itemId, String appendix,
                                                 String[] items, int pageBegin, int pageEnd, int index) {
        List<LearningInstance> instances = new ArrayList<>();
        instances.add(model.featurize(entity, true));
        // predicted various action probability
        // predicted display probability
        // page predicted rating
        List<String> operations = Lists.newArrayList(
                "prediction/display_prob_op",
                "prediction/rate_prob_op",
                "prediction/wishlist_prob_op",
                "prediction/click_prob_op",
                "prediction/rating_pred_op",
                "prediction/GatherNd");
        List<Integer> outputIndices = Lists.newArrayList(0, 0, 0, 0, 0, 0);
        List<String> indices = Lists.newArrayList(
                "MOVIE_ID", "RATE", "WISHLIST", "CLICK", "RATE");
        List<String> attrs = Lists.newArrayList(
                "movieId", "rating", "wishlist", "click", "rating");
        List<String> feaNames = Lists.newArrayList(
                "predDisplay", "predRate", "predWishlist", "predClick", "predRating");
        List<double[][]> predsList = model.inference(instances,
                operations, outputIndices);
        for (int i=0; i<attrs.size(); i++) {
            int itemIdx = model.getIndexForKey(indices.get(i),
                    FeatureExtractorUtilities.composeKey(attrs.get(i), itemId));
            double[] allPreds = predsList.get(i)[0];
            features.put(feaNames.get(i) + appendix, allPreds[itemIdx]);
            DoubleList preds = new DoubleArrayList();
            preds.add(allPreds[itemIdx]);
            double meanPred = allPreds[itemIdx];
            for (int j=pageBegin; j<pageEnd; j++) {
                if (j != index) {
                    itemIdx = model.getIndexForKey(indices.get(i),
                            FeatureExtractorUtilities.composeKey(attrs.get(i), items[j]));
                    preds.add(allPreds[itemIdx]);
                    meanPred += allPreds[itemIdx];
                }
            }
            meanPred /= preds.size();
            features.put(feaNames.get(i) + "Mean" + appendix, meanPred);
            preds.sort(Double::compare);
            features.put(feaNames.get(i) + "Min" + appendix, preds.get(0));
            features.put(feaNames.get(i) + "Max" + appendix, preds.get(preds.size() - 1));
            features.put(feaNames.get(i) + "Median" + appendix, preds.get(preds.size() / 2));
        }
        // user state
        List<String> stateList = new ArrayList<>();
        double[][] states = predsList.get(predsList.size() - 1);
        for (int i=0; i<states[0].length; i++) {
            stateList.add(Double.toString(states[0][i]));
            features.put("state" + Integer.toString(i) + appendix, states[0][i]);
        }
        features.put("state" + appendix, StringUtils.join(stateList, ","));
    }

    static public void extractTensorFlowFeatures(ObjectNode features, Map<String, String[]> attr2seq, int index,
                                                 TensorFlowModel model, int pageBegin, int pageEnd,
                                                 String itemAttr, int closest,
                                                 String userAttr, String userId) {
        // page and closest action similarity/diversity
        String simTarget = "rating";
        String simIndex = "RATE";
        if (weights == null) {
            weights = model.inference("metrics/paras/" + simTarget + "_weights/read", 0);
        }
        String[] items = attr2seq.get(itemAttr + "s");
        String itemId = items[index];
        int itemIdx = model.getIndexForKey(simIndex,
                FeatureExtractorUtilities.composeKey(simTarget, itemId));
        RealVector itemVec = MatrixUtils.createRealVector(weights[itemIdx]);
        features.put("closestSimPage", 0.0);
        DoubleList sims = new DoubleArrayList();
        double meanSim = 0.0;
        for (int i=pageBegin; i<pageEnd; i++) {
            double sim = 1.0;
            if (index != i) {
                int curIdx = model.getIndexForKey(simIndex, FeatureExtractorUtilities.composeKey(simTarget, items[i]));
                RealVector curVec = MatrixUtils.createRealVector(weights[curIdx]);
                sim = itemVec.cosine(curVec);
                sims.add(sim);
                meanSim += sim;
            }
            if (i == closest) {
                features.put("closestSimPage", sim);
            }
        }
        if (sims.size() == 0) {
            sims.add(0.0);
        }
        meanSim /= sims.size();
        features.put("meanSimPage", meanSim);
        sims.sort(Double::compare);
        features.put("minSimPage", sims.getDouble(0));
        features.put("maxSimPage", sims.getDouble(sims.size() - 1));
        features.put("medianSimPage", sims.getDouble(sims.size() / 2));

        // before page and after page predictions
        ObjectNode entity = constructSequence(attr2seq, pageBegin, userAttr, userId);
        extractPredictedFeatures(features, model, entity, itemId, "BeforePage",
                items, pageBegin, pageEnd, index);
        entity = constructSequence(attr2seq, pageEnd, userAttr, userId);
        extractPredictedFeatures(features, model, entity, itemId, "AfterPage",
                items, pageBegin, pageEnd, index);
    }

    static public ObjectNode getFeatures(
            Map<String, String[]> attr2seq, int index,
            TensorFlowModel tensorflow, List<JsonNode> item2info,
            String itemAttr, String userAttr, String userId) {
        ObjectNode features = Json.newObject();
        extractItemLevel(features, attr2seq, index);
        Map.Entry<Integer, Integer> pageRange = extractPageLevel(features, attr2seq, index);
        int closest = extractClosestActionInfo(features, attr2seq, "Page",
                pageRange.getKey(), pageRange.getValue(), index);
        Map.Entry<Integer, Integer> sessRange = extractSessionLevel(features, attr2seq, index, pageRange);
        extractUserLevel(features, attr2seq, index, pageRange, sessRange);

        if (item2info != null) {
            extractItemInfoFeatures(features, attr2seq, index, item2info);
        }
        if (tensorflow != null) {
            extractTensorFlowFeatures(features, attr2seq, index, tensorflow,
                    pageRange.getKey(), pageRange.getValue(), itemAttr, closest,
                    userAttr, userId);
        }
        return features;
    }
}
