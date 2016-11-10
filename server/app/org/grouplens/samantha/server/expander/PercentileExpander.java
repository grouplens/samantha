package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.PercentileModel;
import org.grouplens.samantha.modeler.space.IndexSpace;
import org.grouplens.samantha.modeler.space.SpaceProducer;
import org.grouplens.samantha.modeler.space.VariableSpace;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.common.ModelOperation;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.dao.ExpandedEntityDAO;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class PercentileExpander implements EntityExpander {
    private final List<String> attrNames;
    private final PercentileModel percentileModel;

    public PercentileExpander(List<String> attrNames, PercentileModel percentileModel) {
        this.attrNames = attrNames;
        this.percentileModel = percentileModel;
    }

    static private PercentileModel createModel(Injector injector, String modelName, double sampleRate, int maxNumValues) {
        SpaceProducer spaceProducer = injector.instanceOf(SpaceProducer.class);
        IndexSpace indexSpace = spaceProducer.getIndexSpace(modelName);
        VariableSpace variableSpace = spaceProducer.getVariableSpace(modelName);
        return new PercentileModel(modelName, maxNumValues, sampleRate, indexSpace, variableSpace);
    }

    static private PercentileModel buildModel(PercentileModel model, ModelService modelService,
                                              Configuration attr2config, List<String> attrs,
                                              String engineName, String modelName,
                                              RequestContext requestContext, Injector injector,
                                              Configuration daoConfigs, String daoConfigKey) {
        attrs.parallelStream().forEach(attr -> {
            Configuration config = attr2config.getConfig(attr);
            List<Configuration> expanderConfigs = ExpanderUtilities.getEntityExpandersConfig(config);
            List<EntityExpander> expanders = ExpanderUtilities.getEntityExpanders(requestContext,
                    expanderConfigs, injector);
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    requestContext.getRequestBody().get(daoConfigKey), injector);
            EntityDAO expanded = new ExpandedEntityDAO(expanders, entityDAO, requestContext);
            model.buildModel(attr, config.getInt("numValues"), expanded);
        });
        modelService.setModel(engineName, modelName, model);
        return model;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        ModelService modelService = injector.instanceOf(ModelService.class);
        JsonNode reqBody = requestContext.getRequestBody();
        String engineName = requestContext.getEngineName();
        String modelName = expanderConfig.getString("modelName");
        String modelFile = expanderConfig.getString("modelFile");
        boolean toReset = IOUtilities.whetherModelOperation(modelName, ModelOperation.RESET, reqBody);
        if (toReset) {
            modelService.removeModel(engineName, modelName);
        }
        boolean toBuild = IOUtilities.whetherModelOperation(modelName, ModelOperation.BUILD, reqBody);
        PercentileModel model;
        if (!modelService.hasModel(engineName, modelName) || toBuild) {
            model = createModel(injector, modelName, expanderConfig.getDouble("sampleRate"),
                    expanderConfig.getInt("maxNumValues"));
        } else {
            model = (PercentileModel) modelService.getModel(engineName, modelName);
        }
        if (toBuild) {
            List<String> attrNames = JsonHelpers.getOptionalStringList(reqBody,
                    expanderConfig.getString("attrNamesKey"),
                    expanderConfig.getStringList("attrNames"));
            Configuration daoConfigs = expanderConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
            model = buildModel(model, modelService, expanderConfig.getConfig("attrName2Config"),
                    attrNames, engineName, modelName,
                    requestContext, injector, daoConfigs,
                    expanderConfig.getString("daoConfigKey"));
        }
        boolean toLoad = IOUtilities.whetherModelOperation(modelName, ModelOperation.LOAD, reqBody);
        if (toLoad) {
            model = (PercentileModel) RetrieverUtilities.loadModel(modelService, engineName,
                    modelName, modelFile);
        }
        boolean toDump = IOUtilities.whetherModelOperation(modelName, ModelOperation.DUMP, reqBody);
        if (toDump) {
            RetrieverUtilities.dumpModel(model, modelFile);
        }
        return new PercentileExpander(expanderConfig.getStringList("attrNames"), model);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (String attrName : attrNames) {
            for (ObjectNode entity : initialResult) {
                entity.put(attrName + "Percentile", percentileModel.getPercentile(attrName,
                        entity.get(attrName).asDouble()));
            }
        }
        return initialResult;
    }
}
