package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.predictor.learning.SyncDeserializedLearningData;
import org.grouplens.samantha.server.predictor.learning.SyncFeaturizedLearningData;
import play.Configuration;
import play.inject.Injector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

public class PredictorUtilities {

    private PredictorUtilities() {}

    static public LearningData getLearningData(Featurizer model, RequestContext requestContext,
                                               JsonNode daoConfig, Configuration entityDaoConfigs,
                                               List<Configuration> expandersConfig, Injector injector,
                                               boolean update, String serializedKey, String insAttr,
                                               String labelAttr, String weightAttr, List<String> groupKeys) {
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(entityDaoConfigs, requestContext,
                daoConfig, injector);
        boolean serialized = JsonHelpers.getOptionalBoolean(requestContext.getRequestBody(), serializedKey, false);
        if (serialized) {
            return new SyncDeserializedLearningData(entityDAO, insAttr, groupKeys, labelAttr, weightAttr);
        } else {
            List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                    expandersConfig, injector);
            return new SyncFeaturizedLearningData(entityDAO, groupKeys, entityExpanders,
                    model, requestContext, update);
        }
    }

    static public List<Prediction> predictFromRequest(Predictor predictor, RequestContext requestContext,
                                                      Injector injector, Configuration daoConfigs,
                                                      String daoConfigKey) {
        List<Prediction> predictions = new ArrayList<>();
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                requestContext.getRequestBody().get(daoConfigKey), injector);
        while (entityDAO.hasNextEntity()) {
            ObjectNode entity = entityDAO.getNextEntity();
            List<ObjectNode> entityList = new ArrayList<>();
            entityList.add(entity);
            predictions.addAll(predictor.predict(entityList, requestContext));
        }
        return predictions;
    }

    static public LearningMethod getLearningMethod(Configuration config, Injector injector,
                                                   RequestContext requestContext) {
        String methodClass = config.getString(ConfigKey.METHOD_CLASS.get());
        try {
            Method method = Class.forName(methodClass)
                    .getMethod("getLearningMethod", Configuration.class, Injector.class,
                            RequestContext.class);
            return (LearningMethod) method.invoke(null, config, injector, requestContext);
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    static public ObjectiveFunction getObjectiveFunction(Configuration config, Injector injector,
                                                         RequestContext requestContext) {
        String objectiveClass = config.getString(ConfigKey.OBJECTIVE_CLASS.get());
        try {
            Method method = Class.forName(objectiveClass)
                    .getMethod("getObjectiveFunction", Configuration.class, Injector.class,
                            RequestContext.class);
            return (ObjectiveFunction) method.invoke(null, config, injector, requestContext);
        } catch (IllegalAccessException | InvocationTargetException
                | NoSuchMethodException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }
}
