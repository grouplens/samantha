package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.inject.Injector;

import java.io.*;

abstract public class AbstractModelManager implements ModelManager {
    final protected String modelName;
    final protected String modelFile;
    final protected Injector injector;

    public AbstractModelManager(Injector injector, String modelName, String modelFile) {
        this.injector = injector;
        this.modelName = modelName;
        this.modelFile = modelFile;
    }

    public Object getOrDefaultModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        Object model;
        if (modelService.hasModel(engineName, modelName)) {
            model = modelService.getModel(engineName, modelName);
        } else {
            model = createModel(requestContext);
        }
        return model;
    }

    public Object manage(RequestContext requestContext) {
        JsonNode reqBody = requestContext.getRequestBody();
        if (reqBody.has(ConfigKey.MODEL_NAME_KEY.get()) && reqBody.has(ConfigKey.MODEL_OPERATION_KEY.get()) &&
                modelName.equals(reqBody.get(ConfigKey.MODEL_NAME_KEY.get()).asText())) {
            String operation = reqBody.get(ConfigKey.MODEL_OPERATION_KEY.get()).asText();
            ModelOperation.valueOf(operation).operate(this, requestContext);
        }
        return getOrDefaultModel(requestContext);
    }

    public Object buildModel(Object model, RequestContext requestContext) {
        throw new InvalidRequestException("Building model is not supported");
    }

    public Object updateModel(Object model, RequestContext requestContext) {
        throw new InvalidRequestException("Updating model is not supported");
    }

    public Object buildModel(RequestContext requestContext) {
        Object model = createModel(requestContext);
        buildModel(model, requestContext);
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        modelService.setModel(engineName, modelName, model);
        return model;
    }

    public Object updateModel(RequestContext requestContext) {
        Object model = getOrDefaultModel(requestContext);
        updateModel(model, requestContext);
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        modelService.setModel(engineName, modelName, model);
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
            throw new InvalidRequestException(e);
        }
        return model;
    }

    public Object loadModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        String engineName = requestContext.getEngineName();
        try {
            ObjectInputStream fin = new ObjectInputStream(new FileInputStream(modelFile));
            Object model = fin.readObject();
            fin.close();
            modelService.setModel(engineName, modelName, model);
            return model;
        } catch (IOException | ClassNotFoundException e) {
            throw new InvalidRequestException(e);
        }
    }

    public Object resetModel(RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        Object model = createModel(requestContext);
        modelService.setModel(requestContext.getEngineName(), modelName, model);
        return model;
    }
}
