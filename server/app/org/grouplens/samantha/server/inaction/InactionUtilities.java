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
import play.libs.Json;

import java.util.AbstractMap;
import java.util.Map;

public class InactionUtilities {

    static public final String[] historyAttrs = {
            "notices",
            "tstamps", "sessionIds", "pageNames", "pageSizes", "dwells", "movieIds", "ranks",
            "clicks", "ratings", "highRates", "lowRates", "trailers", "wishlists", "hovers", "stops",
            "positives", "negatives", "actions",
            "reasons", "familiars", "whens", "rates", "futures", "skips",
    };

    /*
    user history level

    how many times the item has been shown, in what context was it displayed, in-system familiarity estimation
        numHistShown, numHistFront, numHistExplore
    the most prominent item display's page information
        mostPromPage, mostPromRow, mostPromCol
    the last item display's page information
        lastPage, lastRow, lastCol
    user tenure and engagement level
        userTenure, items(pages)PerWeek, items(pages)LastWeek
    user activity distribution
        wishlistRate, ratingRate, clickRate, stopRate, actionRate, positiveRate, negativeRate

    session level

    whether the item has been displayed previously in the session
        numSessShown, numSessFront, numSessExplore
    the most prominent display's page information in the session
        mostSessPromRow, mostSessPromCol
    the last item display's page information in the session
        lastSessPage, lastSessRow, lastSessCol
    the previous page information in the session
        prevSessPage, ...
    is this coming back to visit again
        sessPageReview
    how long the session has lasted, engagement level in the session
        sessLength, items(pages)InSess
    user activity distribution in the session
        wishlistSessRate, ratingSessRate, clickSessRate, stopSessRate, actionSessRate,
        positiveSessRate, negativeSessRate

    page view level

    what section and page was the item displayed
        pageName
    how many items were displayed
        pageSize
    what items were displayed together with the item
        maxPredRating
        meanPredRating
        medianPredRating
        minPredRating
    dwell time on the page
        pageDwell
    what position was the item displayed
        row, col
    were there any type of actions on the page
        pageWishlist, pageRating, pageClick, pageStop, pageShortHover, pageLongHover, pageAction,
        pagePositive, pageNegative

    single item level

    were there mouse hover event on the item and other items displayed together
        hover
    predicted rating
        predRating
    other attributes like release year, popularity
        popularity, lastYearPop, releaseYear

    survey response

    if labelAttr is "inaction" which is a combination of notice and skipping, no more set
    if labelAttr is "familiar", no more set
    if labelAttr is "notice", set familiar, when, reason
    if labelAttr is "future", set inaction, familiar, when
    */

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

    static private void extractPageLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        features.put("pageName", attr2seq.get("pageNames")[index]);
        features.put("pageSize", Integer.parseInt(attr2seq.get("pageSizes")[index]));
        features.put("dwell", Float.parseFloat(attr2seq.get("dwells")[index]));
        int rank = Integer.parseInt(attr2seq.get("ranks")[index]);
        features.put("row", rank / 8);
        features.put("col", rank % 8);
        Map.Entry<Integer, Integer> pageRange = getPageRange(index, attr2seq.get("ranks"));
    }

    static private void extractItemLevel(ObjectNode features, Map<String, String[]> attr2seq, int index) {
        features.put("hover", attr2seq.get("hovers")[index]);
    }

    static public ObjectNode getFeatures(
            Map<String, String[]> attr2seq, int index, String labelAttr) {
        ObjectNode features = Json.newObject();
        //extractUserLevel(features);
        extractPageLevel(features, attr2seq, index);
        extractItemLevel(features, attr2seq, index);
        return features;
    }
}
