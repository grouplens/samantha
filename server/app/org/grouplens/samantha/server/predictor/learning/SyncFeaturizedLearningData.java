package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class SyncFeaturizedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final List<EntityExpander> entityExpanders;
    private final Featurizer featurizer;
    private final RequestContext requestContext;
    private int idx = 0;
    private List<ObjectNode> entityList = new ArrayList<>();
    private final boolean update;

    /**
     * @param entityDAO if this is used by multiple SyncFeaturizedLearningData, it needs to be thread-safe by itself.
     */
    public SyncFeaturizedLearningData(EntityDAO entityDAO,
                                      List<EntityExpander> entityExpanders,
                                      Featurizer featurizer,
                                      RequestContext requestContext,
                                      boolean update) {
        this.entityDAO = entityDAO;
        this.entityExpanders = entityExpanders;
        this.featurizer = featurizer;
        this.requestContext = requestContext;
        this.update = update;
    }

    public LearningInstance getLearningInstance() {
        ObjectNode cur;
        do {
            synchronized (this) {
                if (idx < entityList.size()) {
                    cur = entityList.get(idx++);
                    break;
                } else if (entityDAO.hasNextEntity()) {
                    if (entityList.size() == idx) {
                        entityList.clear();
                        idx = 0;
                    }
                    cur = entityDAO.getNextEntity();
                } else {
                    entityDAO.close();
                    return null;
                }
            }
            List<ObjectNode> thrList = new ArrayList<>();
            thrList.add(cur);
            thrList = ExpanderUtilities.expand(thrList, entityExpanders, requestContext);
            synchronized (this) {
                entityList.addAll(thrList);
            }
            cur = null;
        } while (cur == null);
        return featurizer.featurize(cur, update);
    }

    synchronized public void startNewIteration() {
        idx = 0;
        entityList.clear();
        entityDAO.restart();
    }
}
