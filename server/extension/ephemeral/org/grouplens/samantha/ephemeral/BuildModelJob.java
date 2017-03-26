package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.ephemeral.model.CustomSVDFeature;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.common.ModelService;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.indexer.IndexerUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class BuildModelJob implements Job {

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String engineName = dataMap.getString("engineName");
        Injector injector = (Injector) dataMap.get("injector");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);

        Configuration jobConfig = (Configuration) dataMap.get("jobConfig");
        String svdfeaturePredictor = jobConfig.getString("svdfeaturePredictor");
        String svdfeatureModel = jobConfig.getString("svdfeatureModel");

        // Get indexer
        Configuration indexerData = jobConfig.getConfig("indexerData");
        RequestContext indexerPseudoRequest = new RequestContext(
                Json.parse(indexerData.getConfig(ConfigKey.REQUEST_CONTEXT.get())
                        .underlying().root().render(ConfigRenderOptions.concise())),
                indexerData.getString("indexerEngineName", engineName));
        Indexer indexer = configService.getIndexer(indexerData.getString("indexerName"), indexerPseudoRequest);

        // Create a RequestContext with an empty body and the same engine as the indexer
        // The body of the RequestContext isn't get used by any currently written EntityDAOs, so it can be empty
        RequestContext daoPseudoRequest = new RequestContext(Json.newObject(), indexerData.getString("indexerEngineName", engineName));
        JsonNode reqDao = indexer.getIndexedDataDAOConfig(indexerPseudoRequest);
        EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(indexerData.getConfig("entityDaoConfigs"), daoPseudoRequest, reqDao, injector);

        // Get the entityDaoConfigs for this engine (not necessarily the indexer's)
        Configuration entityDaoConfigs = jobConfig.getConfig("entityDaoConfigs");
        String entityDaoKey = entityDaoConfigs.getString("entityDaoKey");
        String entityDaoConfigName =  jobConfig.getString(entityDaoKey);

        List<String> dataFields = jobConfig.getStringList("dataFields");
        String separator = jobConfig.getString("separator", "\t");
        String trainPath = jobConfig.getString("trainPath");
        String valPath = jobConfig.getString("valPath");
        Double valFraction = jobConfig.getDouble("valFraction");

        // Create a set that compares ObjectNodes by userId.movieId keys to ensure uniqueness of ratings.
        // Later ratings will overwrite earlier ratings.
        // TODO: Will need to sort by tstamp if file is not guaranteed to be in order
        Set<ObjectNode> uniqueEntities = new TreeSet<>(Comparator.comparing(x -> x.get("userId").asInt() + "." + x.get("movieId").asInt()));
        while (entityDAO.hasNextEntity()) {
            uniqueEntities.add(entityDAO.getNextEntity());
        }

        Map<Integer, Integer> pop = new HashMap<>();
        try {
            BufferedWriter trainWriter = new BufferedWriter(new FileWriter(trainPath));
            IndexerUtilities.writeOutHeader(dataFields, trainWriter, separator);

            BufferedWriter valWriter = new BufferedWriter(new FileWriter(valPath));
            IndexerUtilities.writeOutHeader(dataFields, valWriter, separator);

            for (ObjectNode obj : uniqueEntities) {
                pop.merge(obj.get("movieId").asInt(), 1, Integer::sum);

                if (Math.random() < valFraction) { // write to validation set
                    IndexerUtilities.writeOutJson(obj, dataFields, valWriter, separator);
                } else { // write to training set
                    IndexerUtilities.writeOutJson(obj, dataFields, valWriter, separator);
                }
            }

            trainWriter.close();
            valWriter.close();

        } catch (IOException e) {
            Logger.error(e.getMessage(), e);
            return;
        }

        // Construct the request body for the build operation
        // Point learningDaoConfig and validationDaoConfig to our newly written files
        ObjectNode learningDaoConfig = Json.newObject();
        learningDaoConfig.put("filePath", trainPath);
        learningDaoConfig.put("separator", separator);
        learningDaoConfig.put(entityDaoKey, entityDaoConfigName);

        ObjectNode validationDaoConfig = Json.newObject();
        validationDaoConfig.put("filePath", valPath);
        validationDaoConfig.put("separator", separator);
        validationDaoConfig.put(entityDaoKey, entityDaoConfigName);

        ObjectNode reqBody = Json.newObject();
        reqBody.put("modelName", svdfeatureModel);
        reqBody.put("predictor", svdfeaturePredictor);
        reqBody.put("modelOperation", "BUILD");
        reqBody.set("learningDaoConfig", learningDaoConfig);
        reqBody.set("validationDaoConfig", validationDaoConfig);

        // Build the model
        RequestContext buildPseudoRequest = new RequestContext(reqBody, engineName);
        configService.getPredictor(svdfeaturePredictor, buildPseudoRequest);

        // Get the model
        ModelService modelService = injector.instanceOf(ModelService.class);
        CustomSVDFeature svdFeature = (CustomSVDFeature) modelService.getModel(engineName, svdfeatureModel);

        // Sort item ids by descending popularity and add them to the model
        List<ObjectNode> entities = pop.entrySet().stream().map(entry -> {
            ObjectNode obj = Json.newObject();
            obj.put("movieId", entry.getKey());
            obj.put("popularity", entry.getValue());
            return obj;
        }).collect(Collectors.toList());
        svdFeature.setEntities(entities);

        // Cache the average user vector
        svdFeature.getAverageUserVector();
    }
}