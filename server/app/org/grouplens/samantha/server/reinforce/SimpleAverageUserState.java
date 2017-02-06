package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.*;
import org.grouplens.samantha.server.common.*;
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

public class SimpleAverageUserState implements Transitioner, EntityExpander {
    final private TransitionModelManager modelManager;
    final private boolean update;

    public SimpleAverageUserState(TransitionModelManager modelManager, boolean update) {
        this.update = update;
        this.modelManager = modelManager;
    }

    static private class TransitionModelManager extends AbstractModelManager {
        private final List<String> actionAttrs;
        private final List<String> stateKeys;
        private final String daoConfigKey;
        private final Configuration daoConfigs;
        private final List<Configuration> expandersConfig;

        public TransitionModelManager(String modelName, String modelFile, Configuration daoConfigs,
                                      List<Configuration> expandersConfig, String daoConfigKey,
                                      List<String> actionAttrs, List<String> stateKeys, Injector injector) {
            super(injector, modelName, modelFile, new ArrayList<>());
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
            RealVector vec = MatrixUtils.createRealVector(new double[actionAttrs.size()]);
            for (int i=0; i< actionAttrs.size(); i++) {
                vec.setEntry(i, action.get(actionAttrs.get(i)).asDouble());
            }
            return vec;
        }

        private RealVector getStateValue(ObjectNode state) {
            RealVector vec = MatrixUtils.createRealVector(new double[actionAttrs.size() + 1]);
            double cnt = state.get(modelName + "-state-cnt").asDouble();
            vec.setEntry(0, cnt);
            for (int i=0; i< actionAttrs.size(); i++) {
                vec.setEntry(i + 1, state.get("state-" + actionAttrs.get(i)).asDouble() * cnt);
            }
            return vec;
        }

        public void expand(ObjectNode state, RequestContext requestContext, boolean update) {
            IndexedVectorModel stateModel = getModel(requestContext);
            RealVector curVal = getState(stateModel, state);
            setState(state, curVal);
            ObjectNode newState = Json.newObject();
            RealVector newVal = getNewState(curVal, state);
            setState(newState, newVal);
            if (update) {
                String stateKey = getStateKey(state);
                stateModel.ensureKey(stateKey);
                stateModel.setKeyVector(stateKey, newVal);
            }
        }

        private IndexedVectorModel getModel(RequestContext requestContext) {
            return (IndexedVectorModel) getOrDefaultModel(requestContext);
        }

        public List<ObjectNode> transition(ObjectNode state, ObjectNode action) {
            List<ObjectNode> newStates = new ArrayList<>(1);
            ObjectNode newState = Json.newObject();
            newState.put(ConfigKey.STATE_PROBABILITY_NAME.get(), 1.0);
            RealVector curVal = getStateValue(state);
            RealVector newVal = getNewState(curVal, action);
            setState(newState, newVal);
            newStates.add(newState);
            return newStates;
        }

        private void setState(ObjectNode state, RealVector value) {
            double cnt = value.getEntry(0);
            state.put(modelName + "-state-cnt", cnt);
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

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName, spaceMode);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName, spaceMode);
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
            entityDAO.close();
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
        return new SimpleAverageUserState(modelManager, update);
    }

    public List<ObjectNode> transition(ObjectNode state, ObjectNode action) {
        return modelManager.transition(state, action);
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return (EntityExpander) getTransitioner(expanderConfig, injector, requestContext);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            modelManager.expand(entity, requestContext, update);
        }
        return initialResult;
    }
}
