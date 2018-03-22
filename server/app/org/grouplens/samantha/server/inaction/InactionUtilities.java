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

import java.util.Map;

public class InactionUtilities {

    static public final String[] historyAttrs = {
            "sessionIds", "movieIds", "tstamps", "ranks", "ratings",
            "pageNames", "clicks", "stops", "wishlists", "dwells", "hovers",
            "reasons", "notices", "familiars", "whens", "rates", "skips", "futures",
    };

    /*
    user history level

    how many times the item has been shown, in what context was it displayed, in-system familiarity estimation
        numHistShown, numHistFront, numHistExplore, mostPromRow, mostPromCol, prevPage, prevRow, prevCol
    user tenure and engagement level
        userTenure, items(pages)PerWeek, items(pages)LastWeek
    user activity distribution
        wishlistRate, ratingRate, clickRate, stopRate, actionRate, positiveRate, negativeRate

    session level

    whether the item has been displayed previously in the session, in what context
        numSessShown, numSessFront, numSessExplore, minSessRank, prevSessPage, prevSessRank
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
        numPageItems
    what items were displayed together with the item
    dwell time on the page
        pageDwell
    what position was the item displayed
        row, col
    were there any type of actions on the page
        pageWishlist, pageRating, pageClick, pageStop, pageShortHover, pageLongHover, pageAction,

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
    static public ObjectNode getFeatures(
            Map<String, String[]> attr2seq, int index, String user, String item, String labelAttr) {
        // if index is negative, extract default features
        return Json.newObject();
    }
}
