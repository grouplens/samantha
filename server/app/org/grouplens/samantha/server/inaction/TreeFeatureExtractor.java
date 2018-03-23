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
import com.google.common.collect.Lists;
import org.grouplens.samantha.modeler.featurizer.Feature;
import org.grouplens.samantha.modeler.featurizer.IdentityExtractor;
import org.grouplens.samantha.modeler.featurizer.StringValueExtractor;
import org.grouplens.samantha.modeler.model.IndexSpace;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TreeFeatureExtractor implements org.grouplens.samantha.modeler.featurizer.FeatureExtractor {
    private static final long serialVersionUID = 1L;
    private final String labelAttr;
    private final String labelClass;

    public TreeFeatureExtractor(String labelAttr, String labelClass) {
        this.labelAttr = labelAttr;
        this.labelClass = labelClass;
    }

    public Map<String, List<Feature>> extract(JsonNode entity, boolean update,
                                              IndexSpace indexSpace) {
        Map<String, List<Feature>> feaMap = new HashMap<>();

        /*
        String[] catFeas = {
                "namePage",
                "closestActionPage",
                "movieId",
                "userId",
                "namePrev",
        };
        */
        String[] catFeas = {};

        String[] numFeas = {
                "actionPage", "lowRatePage", "colPage", "dwellPage", "ratingPage", "negativeRatePage",
                "hoverRatePage", "highRatePage", "closestInvDistPage", "wishlistRatePage", "positiveRatePage",
                "actionRatePage", "closestRowPage", "rowPage", "clickPage", "hoverPage", "sizePage", "clickRatePage",
                "positivePage", "closestColPage", "wishlistPage", "lowRateRatePage",
                "stopRatePage", "stopPage", "highRateRatePage", "negativePage", "ratingRatePage",
                "numFrontShowOutSess", "stopSess", "clickSess", "invReviewDistOutSess", "dwellPrevShowSess",
                "clickRateSess", "lengthSess", "numShowSess", "stopRateSess", "namePrevShowSess", "highRateRateSess",
                "numReviewSess", "hoverSess", "lowRateSess", "rowPrevShowSess", "numFrontShowSess", "actionRateSess",
                "numExploreShowOutSess", "actionSess", "numShowOutSess", "wishlistSess", "invReviewDistSess",
                "highRateSess", "lowRateRateSess", "numExploreShowSess", "numReviewOutSess", "wishlistRateSess",
                "ratingRateSess", "positiveRateSess", "sizePrevShowSess", "hoverRateSess", "meanNumItemSess",
                "numItemSess", "negativeRateSess", "positiveSess", "colPrevShowSess", "ratingSess", "negativeSess",
                "meanNumItemHist", "actionHist", "highRateHist", "actionRateHist", "numItemHist", "wishlistRateHist",
                "hoverRateHist", "hoverHist", "numFrontShowHist", "wishlistHist", "positiveRateHist", "numReviewHist",
                "lengthHist", "lowRateRateHist", "negativeHist", "clickRateHist", "stopHist", "positiveHist",
                "highRateRateHist", "invReviewDistHist", "stopRateHist", "ratingHist", "lowRateHist",
                "ratingRateHist", "negativeRateHist", "numShowHist", "clickHist", "numExploreShowHist",
                "sizePrev", "dwellPrev", "colPrev", "rowPrev", "hover",
        };

        for (String cat : catFeas) {
            StringValueExtractor catExt = new StringValueExtractor("TREE", cat, cat);
            feaMap.putAll(catExt.extract(entity, update, indexSpace));
        }
        for (String num : numFeas) {
            IdentityExtractor numExt = new IdentityExtractor("TREE", num, num);
            feaMap.putAll(numExt.extract(entity, update, indexSpace));
        }
        StringValueExtractor labelExt = new StringValueExtractor("CLASS", labelAttr, labelClass);
        feaMap.putAll(labelExt.extract(entity, update, indexSpace));
        return feaMap;
    }
}
