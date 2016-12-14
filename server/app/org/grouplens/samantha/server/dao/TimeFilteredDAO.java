package org.grouplens.samantha.server.dao;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;

public class TimeFilteredDAO implements EntityDAO {
    private final int beginTime;
    private final int endTime;
    private final String timestampField;
    private final EntityDAO entityDAO;
    private ObjectNode cur;

    public TimeFilteredDAO(EntityDAO entityDAO, int beginTime, int endTime,
                           String timestampField) {
        this.beginTime = beginTime;
        this.endTime = endTime;
        this.entityDAO = entityDAO;
        this.timestampField = timestampField;
    }

    public boolean hasNextEntity() {
        if (cur != null) {
            return true;
        }
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            int tstamp = entity.get(timestampField).asInt();
            if (tstamp >= beginTime && tstamp <= endTime) {
                cur = entity;
                return true;
            }
        }
        return false;
    }

    public ObjectNode getNextEntity() {
        ObjectNode ret;
        if (cur != null) {
            ret = cur;
            cur = null;
            return ret;
        } else {
            if (hasNextEntity()) {
                return cur;
            } else {
                return null;
            }
        }
    }

    public void restart() {
        entityDAO.restart();
        cur = null;
    }

    public void close() {
        entityDAO.close();
        cur = null;
    }
}
