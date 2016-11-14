package org.grouplens.samantha.server.predictor.learning;

import com.fasterxml.jackson.databind.node.ObjectNode;

import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.common.LearningInstance;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;

import java.util.ArrayList;
import java.util.List;

public class FeaturizedLearningData implements LearningData {
    private final EntityDAO entityDAO;
    private final List<EntityExpander> entityExpanders;
    private final Featurizer featurizer;
    private final RequestContext requestContext;
    private int idx = 0;
    private List<ObjectNode> entityList = new ArrayList<>();
    private final boolean update;

    public FeaturizedLearningData(EntityDAO entityDAO,
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
        if (idx < entityList.size()) {
            return featurizer.featurize(entityList.get(idx++), update);
        } else if (entityDAO.hasNextEntity()) {
            ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList, entityExpanders, requestContext);
            if (entityList.size() > 0) {
                idx = 0;
                return featurizer.featurize(entityList.get(idx++), update);
            } else {
                entityDAO.close();
                return null;
            }
        } else {
            entityDAO.close();
            return null;
        }
    }

    public void startNewIteration() {
        idx = 0;
        entityList.clear();
        entityDAO.restart();
    }
}
