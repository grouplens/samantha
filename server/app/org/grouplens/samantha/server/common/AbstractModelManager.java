package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.space.SpaceMode;
import org.grouplens.samantha.modeler.space.SpaceModel;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.evaluator.Evaluator;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.io.*;
import java.util.List;

abstract public class AbstractModelManager implements ModelManager {
    final protected String modelName;
    final protected String modelFile;
    final protected Injector injector;
    final protected List<String> evaluatorNames;

    public AbstractModelManager(Injector injector, String modelName,
                                String modelFile, List<String> evaluatorNames) {
        this.injector = injector;
        this.modelName = modelName;
        this.modelFile = modelFile;
        this.evaluatorNames = evaluatorNames;
    }

    protected Object getOrDefaultModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        Object model;
        if (modelService.hasModel(engineName, modelName)) {
            model = modelService.getModel(engineName, modelName);
        } else {
            model = createModel(requestContext, SpaceMode.DEFAULT);
            modelService.setModel(engineName, modelName, model);
        }
        return model;
    }

    public Object manage(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        if (reqBody.has(ConfigKey.MODEL_NAME.get()) && reqBody.has(ConfigKey.MODEL_OPERATION.get()) &&
                modelName.equals(reqBody.get(ConfigKey.MODEL_NAME.get()).asText())) {
            String operation = reqBody.get(ConfigKey.MODEL_OPERATION.get()).asText();
            return ModelOperation.valueOf(operation).operate(this, requestContext);
        } else {
            return getOrDefaultModel(requestContext);
        }
    }

    public Object buildModel(Object model, RequestContext requestContext) {
        throw new BadRequestException("Building model is not supported");
    }

    public Object updateModel(Object model, RequestContext requestContext) {
        throw new BadRequestException("Updating model is not supported");
    }

    public Object buildModel(RequestContext requestContext) {
        Object model = createModel(requestContext, SpaceMode.BUILDING);
        String engineName = requestContext.getEngineName();
        ModelService modelService = injector.instanceOf(ModelService.class);
        modelService.setModel(engineName, ConfigKey.MODEL_EVALUATING_PREFIX.get() + modelName, model);
        buildModel(model, requestContext);
        if (passModel(model, requestContext)) {
            if (model instanceof SpaceModel) {
                ((SpaceModel) model).publishModel();
            }
            modelService.setModel(engineName, modelName, model);
            modelService.removeModel(engineName, ConfigKey.MODEL_EVALUATING_PREFIX.get() + modelName);
        } else {
            throw new BadRequestException("Building model " + modelName + " did not pass evaluation.");
        }
        return model;
    }

    public Object evaluateModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        if (modelService.hasModel(engineName, ConfigKey.MODEL_EVALUATING_PREFIX.get() + modelName)) {
            return modelService.getModel(engineName, ConfigKey.MODEL_EVALUATING_PREFIX.get() + modelName);
        } else {
            return modelService.getModel(engineName, modelName);
        }
    }

    public boolean passModel(Object model, RequestContext requestContext) {
        if (evaluatorNames == null || evaluatorNames.size() == 0) {
            return true;
        }
        String engineName = requestContext.getEngineName();
        boolean pass = true;
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        ObjectNode pseudoReqBody = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), pseudoReqBody);
        pseudoReqBody.put(ConfigKey.MODEL_OPERATION.get(), ModelOperation.EVALUATE.get());
        RequestContext pseudoReq = new RequestContext(pseudoReqBody, engineName);
        for (String name : evaluatorNames) {
            Evaluator evaluator = configService.getEvaluator(name, pseudoReq);
            if (!evaluator.evaluate(pseudoReq).getPass()) {
                pass = false;
            }
        }
        return pass;
    }

    public Object updateModel(RequestContext requestContext) {
        Object model = getOrDefaultModel(requestContext);
        updateModel(model, requestContext);
        return model;
    }

    public Object dumpModel(RequestContext requestContext) {
        Object model = getOrDefaultModel(requestContext);
        try {
            String tmpFile = modelFile + ".tmp";
            ObjectOutputStream fout = new ObjectOutputStream(new FileOutputStream(tmpFile));
            fout.writeObject(model);
            fout.close();
            File file = new File(tmpFile);
            file.renameTo(new File(modelFile));
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        return model;
    }

    public Object loadModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        String toLoadFile = JsonHelpers.getOptionalString(requestContext.getRequestBody(),
                ConfigKey.MODEL_FILE.get(), modelFile);
        try {
            ObjectInputStream fin = new ObjectInputStream(new FileInputStream(toLoadFile));
            Object model = fin.readObject();
            fin.close();
            modelService.setModel(engineName, modelName, model);
            return model;
        } catch (IOException | ClassNotFoundException e) {
            throw new BadRequestException(e);
        }
    }

    public Object resetModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        Object model = createModel(requestContext, SpaceMode.DEFAULT);
        modelService.setModel(requestContext.getEngineName(), modelName, model);
        return model;
    }
}
