package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import play.libs.Json;

public class ItemIDListDAO implements EntityDAO {
    private int iter = 0;
    private final ArrayNode itemList;
    private final String attrName;

    public ItemIDListDAO(ArrayNode itemList, String attrName) {
        this.itemList = itemList;
        this.attrName = attrName;
    }

    public boolean hasNextEntity() {
        if (iter >= itemList.size()) {
            return false;
        } else {
            return true;
        }
    }

    public ObjectNode getNextEntity() {
        ObjectNode obj = Json.newObject();
        obj.set(attrName, itemList.get(iter++));
        return obj;
    }

    public void restart() {
        iter = 0;
    }

    public void close() {}
}
