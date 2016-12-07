package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.featurizer.FeaturizerUtilities;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class SyncFeaturizedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final GroupedEntityList groupedEntityList;
    private final List<EntityExpander> entityExpanders;
    private final List<String> groupKeys;
    private final Featurizer featurizer;
    private final RequestContext requestContext;
    private final boolean update;

    public SyncFeaturizedLearningData(EntityDAO entityDAO,
                                      List<String> groupKeys,
                                      List<EntityExpander> entityExpanders,
                                      Featurizer featurizer,
                                      RequestContext requestContext,
                                      boolean update) {
        this.entityDAO = entityDAO;
        this.entityExpanders = entityExpanders;
        this.featurizer = featurizer;
        this.requestContext = requestContext;
        this.update = update;
        this.groupKeys = groupKeys;
        if (groupKeys != null && groupKeys.size() > 0) {
            groupedEntityList = new GroupedEntityList(groupKeys, entityDAO);
        } else {
            groupedEntityList = null;
        }
    }

    public List<LearningInstance> getLearningInstance() {
        if (groupedEntityList == null) {
            List<ObjectNode> curList;
            do {
                synchronized (entityDAO) {
                    if (entityDAO.hasNextEntity()) {
                        curList = new ArrayList<>();
                        curList.add(entityDAO.getNextEntity());
                    } else {
                        entityDAO.close();
                        return new ArrayList<>(0);
                    }
                }
                curList = ExpanderUtilities.expand(curList, entityExpanders, requestContext);
            } while (curList.size() == 0);
            return FeaturizerUtilities.featurize(curList, groupKeys, featurizer, update);
        } else {
            List<ObjectNode> entityList;
            do {
                entityList = groupedEntityList.getNextGroup();
                if (entityList.size() == 0) {
                    groupedEntityList.close();
                    return new ArrayList<>(0);
                }
                entityList = ExpanderUtilities.expand(entityList, entityExpanders, requestContext);
            } while (entityList.size() == 0);
            return FeaturizerUtilities.featurize(entityList, groupKeys, featurizer, update);
        }
    }

    synchronized public void startNewIteration() {
        if (groupedEntityList == null) {
            entityDAO.restart();
        } else {
            groupedEntityList.restart();
        }
    }
}
