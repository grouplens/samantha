package org.grouplens.samantha.ephemeral;


import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class RandomDAO implements EntityDAO {
    final private static Logger logger = LoggerFactory.getLogger(RandomDAO.class);
    private final EntityDAO entityDAO;
    private long cnt;
    private Random rand;
    private List<ObjectNode> objects;

    public RandomDAO(EntityDAO entityDAO) {
        this.entityDAO = entityDAO;
        this.rand = new Random();
        this.cnt = 0;
        this.objects = null;
    }

    public boolean hasNextEntity() {
        // Make sure we've cached the items from the underlying DAO
        if (objects == null) {
            cacheObjects();
        }

        // Only return up to the number of entities in the underlying DAO
        // Then require that restart() is called.
        if (cnt < objects.size()) {
            return true;
        } else {
            return false;
        }
    }

    public ObjectNode getNextEntity() {
        if (hasNextEntity()) {
            ObjectNode obj = objects.get(rand.nextInt(objects.size()));
            cnt++;
            return obj;
        } else {
            throw new NoSuchElementException();
        }
    }

    public void restart() {
        logger.info("Restarting RandomDAO");
        cnt = 0;
    }

    public void close() {
        logger.info("Closing RandomDAO");
        objects = null;
    }

    private void cacheObjects() {
        logger.info("Caching EntityDAO objects");
        objects = new ArrayList<>();

        while (entityDAO.hasNextEntity()) {
            objects.add(entityDAO.getNextEntity());
        }

        entityDAO.close();
    }
}
