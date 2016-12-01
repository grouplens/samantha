package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.common.*;
import org.grouplens.samantha.modeler.space.IndexedVectorModel;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class SimpleAverageTransitioner implements Transitioner {
    final private RequestContext requestContext;
    final private TransitionModelManager modelManager;
    final private boolean update;

    public SimpleAverageTransitioner(TransitionModelManager modelManager, boolean update,
                                     RequestContext requestContext) {
        this.update = update;
        this.modelManager = modelManager;
        this.requestContext = requestContext;
    }

    static private class TransitionModelManager extends AbstractModelManager {
        final private List<String> actionAttrs;
        final private List<String> stateKeys;
        final private String daoConfigKey;
        private final Configuration daoConfigs;
        private final List<Configuration> expandersConfig;

        public TransitionModelManager(String modelName, String modelFile, Configuration daoConfigs,
                                      List<Configuration> expandersConfig, String daoConfigKey,
                                      List<String> actionAttrs, List<String> stateKeys, Injector injector) {
            super(injector, modelName, modelFile);
            this.actionAttrs = actionAttrs;
            this.stateKeys = stateKeys;
            this.daoConfigKey = daoConfigKey;
            this.daoConfigs = daoConfigs;
            this.expandersConfig = expandersConfig;
        }

        private String getStateKey(ObjectNode state) {
            List<String> multiples = new ArrayList<>();
            for (String attr : stateKeys) {
                multiples.add(FeatureExtractorUtilities.composeKey(attr, state.get(attr).asText()));
            }
            return FeatureExtractorUtilities.composeKey(multiples);
        }

        private RealVector getState(IndexedVectorModel stateModel, ObjectNode state) {
            String stateKey = getStateKey(state);
            RealVector curVal;
            if (stateModel.hasKey(stateKey)) {
                curVal = stateModel.getKeyVector(stateKey);
            } else {
                curVal = MatrixUtils.createRealVector(new double[actionAttrs.size() + 1]);
            }
            return curVal;
        }

        private RealVector getNewState(RealVector current, ObjectNode action) {
            RealVector curVal = current.copy();
            RealVector actionStateValue = getActionStateValue(action);
            curVal.setEntry(0, curVal.getEntry(0) + 1);
            curVal.setSubVector(1, curVal.getSubVector(1, actionStateValue.getDimension())
                    .add(actionStateValue));
            return curVal;
        }

        private RealVector getActionStateValue(ObjectNode action) {
            RealVector arrayList = MatrixUtils.createRealVector(new double[actionAttrs.size()]);
            for (int i=0; i< actionAttrs.size(); i++) {
                arrayList.setEntry(i, action.get(actionAttrs.get(i)).asDouble());
            }
            return arrayList;
        }

        public List<ObjectNode> getCurrentAndNewStates(ObjectNode state, ObjectNode action,
                                                       boolean update, RequestContext requestContext) {
            List<ObjectNode> entityList = new ArrayList<>();
            entityList.add(state);
            List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                    expandersConfig, injector);
            entityList = ExpanderUtilities.expand(entityList, expanders, requestContext);
            if (entityList.size() > 0) {
                state = entityList.get(0);
            }
            IndexedVectorModel stateModel = (IndexedVectorModel) getOrDefaultModel(requestContext);
            RealVector curVal = getState(stateModel, state);
            setState(state, curVal);
            List<ObjectNode> newStates = new ArrayList<>(1);
            ObjectNode newState = Json.newObject();
            newState.put(ConfigKey.STATE_PROBABILITY_NAME.get(), 1.0);
            RealVector newVal = getNewState(curVal, action);
            setState(newState, newVal);
            newStates.add(newState);
            if (update) {
                String stateKey = getStateKey(state);
                stateModel.ensureKey(stateKey);
                stateModel.setKeyVector(stateKey, newVal);
            }
            return newStates;
        }

        private void setState(ObjectNode state, RealVector value) {
            double cnt = value.getEntry(0);
            for (int i=0; i< actionAttrs.size(); i++) {
                if (!state.has("state-" + actionAttrs.get(i))) {
                    if (cnt != 0.0) {
                        state.put("state-" + actionAttrs.get(i), value.getEntry(i + 1) / cnt);
                    } else {
                        state.put("state-" + actionAttrs.get(i), 0.0);
                    }
                }
            }
        }

        public Object createModel(RequestContext requestContext) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
            return new IndexedVectorModel(modelName, 10, actionAttrs.size() + 1, indexSpace, variableSpace);
        }

        public Object updateModel(Object model, ObjectNode entity) {
            IndexedVectorModel stateModel = (IndexedVectorModel) model;
            RealVector curVal = getState(stateModel, entity);
            RealVector newVal = getNewState(curVal, entity);
            String stateKey = getStateKey(entity);
            stateModel.ensureKey(stateKey);
            stateModel.setKeyVector(stateKey, newVal);
            return model;
        }

        public Object updateModel(Object model, RequestContext requestContext) {
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    JsonHelpers.getRequiredJson(requestContext.getRequestBody(), daoConfigKey), injector);
            List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                    expandersConfig, injector);
            while (entityDAO.hasNextEntity()) {
                ObjectNode entity = entityDAO.getNextEntity();
                List<ObjectNode> entityList = new ArrayList<>();
                entityList.add(entity);
                entityList = ExpanderUtilities.expandFromEntityDAO(entityDAO, entityList,
                        expanders, requestContext);
                if (entityList.size() > 0) {
                    for (ObjectNode state : entityList) {
                        updateModel(model, state);
                    }
                }
            }
            return model;
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            return updateModel(model, requestContext);
        }
    }

    public static Transitioner getTransitioner(Configuration config,
                                               Injector injector, RequestContext requestContext) {
        String modelName = config.getString("stateModelName");
        String modelFile = config.getString("stateModelFile");
        List<String> actionAttrs = config.getStringList("actionAttrs");
        String updateKey = config.getString("updateKey");
        JsonNode reqBody = requestContext.getRequestBody();
        boolean update = JsonHelpers.getOptionalBoolean(reqBody, updateKey, false);
        Configuration daoConfigs = config.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        List<Configuration> expanders = ExpanderUtilities.getEntityExpandersConfig(config);
        String daoConfigKey = config.getString("daoConfigKey");
        TransitionModelManager modelManager = new TransitionModelManager(modelName, modelFile, daoConfigs, expanders,
                daoConfigKey, actionAttrs, config.getStringList("stateKeys"), injector);
        modelManager.manage(requestContext);
        return new SimpleAverageTransitioner(modelManager, update, requestContext);
    }

    public List<ObjectNode> transition(ObjectNode state, ObjectNode action) {
        return modelManager.getCurrentAndNewStates(state, action, update, requestContext);
    }
}
