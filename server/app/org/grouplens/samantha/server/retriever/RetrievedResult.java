package org.grouplens.samantha.server.retriever;

import com.fasterxml.jackson.databind.node.ObjectNode;

import java.util.List;

public class RetrievedResult {
    private List<ObjectNode> entityList;
    private long maxHits;

    public RetrievedResult(List<ObjectNode> entityList, long maxHits) {
        this.entityList = entityList;
        this.maxHits = maxHits;
    }

    public List<ObjectNode> getEntityList() {
        return entityList;
    }

    List<ObjectNode> setEntityList(List<ObjectNode> newList) {
        this.entityList = newList;
        return newList;
    }

    public void setMaxHits(long maxHits) {
        this.maxHits = maxHits;
    }

    public long getMaxHits() {
        return maxHits;
    }
}
