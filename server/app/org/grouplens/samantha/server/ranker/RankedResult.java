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
    final private JsonNode params;

    public RankedResult(List<Prediction> ranking, int offset, int limit, long maxHits) {
        this.ranking = ranking;
        this.offset = offset;
        this.limit = limit;
        this.maxHits = maxHits;
        this.params = null;
    }

    public RankedResult(List<Prediction> ranking, int offset, int limit, long maxHits, JsonNode params) {
        this.ranking = ranking;
        this.offset = offset;
        this.limit = limit;
        this.maxHits = maxHits;
        this.params = params;
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
        if (params != null) {
            obj.set("params", params);
        }
        return obj;
    }
}
