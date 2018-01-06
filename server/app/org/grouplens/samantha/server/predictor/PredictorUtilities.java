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

package org.grouplens.samantha.server.predictor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.common.LearningMethod;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.Featurizer;
import org.grouplens.samantha.modeler.common.LearningData;
import org.grouplens.samantha.modeler.solver.ObjectiveFunction;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
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
                                               boolean update, List<String> groupKeys, Integer batchSize) {
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(entityDaoConfigs, requestContext,
                daoConfig, injector);
        List<EntityExpander> entityExpanders = ExpanderUtilities.getEntityExpanders(requestContext,
                expandersConfig, injector);
        return new SyncFeaturizedLearningData(
                entityDAO, groupKeys, batchSize, entityExpanders, model, requestContext, update);
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
