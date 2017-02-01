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

package org.grouplens.samantha.server.ranker;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.predictor.Prediction;
import play.libs.Json;

import java.util.List;

public class RankedResult {
    final private List<Prediction> ranking;
    final private int offset;
    final private int limit;
    final private long maxHits;

    public RankedResult(List<Prediction> ranking, int offset, int limit, long maxHits) {
        this.ranking = ranking;
        this.offset = offset;
        this.limit = limit;
        this.maxHits = maxHits;
    }

    public List<Prediction> getRankingList() {
        return ranking;
    }

    public int getOffset() {
        return offset;
    }

    public int getLimit() {
        return limit;
    }

    public JsonNode toJson() {
        ObjectNode obj = Json.newObject();
        obj.put("limit", limit);
        obj.put("offset", offset);
        obj.put("maxHits", maxHits);
        ArrayNode arr = Json.newArray();
        for (Prediction prediction : ranking) {
            arr.add(prediction.toJson());
        }
        obj.set("ranking", arr);
        return obj;
    }
}
