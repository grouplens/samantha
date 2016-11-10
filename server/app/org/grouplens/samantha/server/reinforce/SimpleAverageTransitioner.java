package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealVector;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.modeler.space.IndexedVectorModel;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class SimpleAverageTransitioner implements Transitioner {
    final private IndexedVectorModel stateModel;
    final private List<String> stateKeys;
    final private List<String> actionAttrs;
    final private boolean update;

    public SimpleAverageTransitioner(IndexedVectorModel stateModel, List<String> stateKeys,
                                     List<String> actionAttrs, boolean update) {
        this.stateModel = stateModel;
        this.stateKeys = stateKeys;
        this.actionAttrs = actionAttrs;
        this.update = update;
    }

    public static Transitioner getTransitioner(Configuration config,
                                               Injector injector, RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        String modelName = config.getString("stateModelName");
        String modelFile = config.getString("stateModelFile");
        List<String> actionAttrs = config.getStringList("actionAttrs");
        String updateKey = config.getString("updateKey");
        JsonNode reqBody = requestContext.getRequestBody();
        boolean update = JsonHelpers.getOptionalBoolean(reqBody, updateKey, false);
        boolean toBuild = IOUtilities.whetherModelOperation(modelName, ModelOperation.BUILD, reqBody);
        if (!modelService.hasModel(engineName, modelName) || toBuild) {
            SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
            IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
            VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
            modelService.setModel(engineName, modelName,
                    new IndexedVectorModel(modelName, 10, actionAttrs.size() + 1, indexSpace, variableSpace));
        }
        IndexedVectorModel stateModel = (IndexedVectorModel) modelService.getModel(engineName, modelName);
        boolean toLoad = IOUtilities.whetherModelOperation(modelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            stateModel = (IndexedVectorModel) RetrieverUtilities.loadModel(modelService, engineName, modelName,
                    modelFile);
        }
        boolean toDump = IOUtilities.whetherModelOperation(modelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(stateModel, modelFile);
        }
        return new SimpleAverageTransitioner(stateModel, config.getStringList("stateKeys"), actionAttrs, update);
    }

    private String getStateKey(ObjectNode state) {
        List<String> multiples = new ArrayList<>();
        for (String attr : stateKeys) {
            multiples.add(FeatureExtractorUtilities.composeKey(attr, state.get(attr).asText()));
        }
        return FeatureExtractorUtilities.composeKey(multiples);
    }

    private RealVector getActionStateValue(ObjectNode action) {
        RealVector arrayList = MatrixUtils.createRealVector(new double[actionAttrs.size()]);
        for (int i=0; i< actionAttrs.size(); i++) {
            arrayList.setEntry(i, action.get(actionAttrs.get(i)).asDouble());
        }
        return arrayList;
    }

    private void setCurrentState(ObjectNode state, RealVector value) {
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

    synchronized public List<ObjectNode> transition(ObjectNode state, ObjectNode action) {
        ObjectNode newState = Json.newObject();
        newState.put(ConfigKey.STATE_PROBABILITY_NAME.get(), 1.0);
        String stateKey = getStateKey(state);
        RealVector curVal;
        if (stateModel.hasKey(stateKey)) {
            curVal = stateModel.getKeyVector(stateKey);
        } else {
            curVal = MatrixUtils.createRealVector(new double[actionAttrs.size() + 1]);
        }
        setCurrentState(state, curVal);
        List<ObjectNode> newStates = new ArrayList<>(1);
        if (update) {
            RealVector actionStateValue = getActionStateValue(action);
            curVal.setEntry(0, curVal.getEntry(0) + 1);
            curVal.setSubVector(1, curVal.getSubVector(1, actionStateValue.getDimension())
                    .add(actionStateValue));
            stateModel.ensureKey(stateKey);
            stateModel.setKeyVector(stateKey, curVal);
            setCurrentState(newState, curVal);
            newStates.add(newState);
        }
        return newStates;
    }
}
