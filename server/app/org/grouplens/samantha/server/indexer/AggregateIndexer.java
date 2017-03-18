package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.GroupedEntityList;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class AggregateIndexer extends AbstractIndexer {
    private final GroupedIndexer indexer;
    private final List<String> otherFields;
    private final String daoNameKey;
    private final String daoName;
    private final String filesKey;
    private final String separator;
    private final String separatorKey;
    private final List<String> groupKeys;
    private final String filePath;
    private final String filePathKey;
    private final List<String> aggFields;
    private final String aggCntName;
    private final String aggSumAppendix;
    private final List<String> dataFields;

    public AggregateIndexer(SamanthaConfigService configService,
                            Configuration config, Injector injector, Configuration daoConfigs,
                            String daoConfigKey, String filePathKey,
                            List<String> otherFields, String separator,
                            String daoNameKey, String daoName, String filesKey,
                            List<String> groupKeys, String filePath,
                            String separatorKey, GroupedIndexer indexer,
                            List<String> aggFields, String aggCntName, String aggSumAppendix) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.filePathKey = filePathKey;
        this.otherFields = otherFields;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.filesKey = filesKey;
        this.separatorKey = separatorKey;
        this.separator = separator;
        this.groupKeys = groupKeys;
        this.filePath = filePath;
        this.aggCntName = aggCntName;
        this.aggFields = aggFields;
        this.aggSumAppendix = aggSumAppendix;
        this.dataFields = new ArrayList<>();
        this.dataFields.add(aggCntName);
        this.dataFields.addAll(otherFields);
        for (String aggField : aggFields) {
            this.dataFields.add(aggField + aggSumAppendix);
        }
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        EntityDAO data = indexer.getEntityDAO(requestContext);
        GroupedEntityList groupDao = new GroupedEntityList(groupKeys, data);
        try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(filePath));
            IndexerUtilities.writeOutHeader(otherFields, writer, separator);
            List<ObjectNode> acts;
            while ((acts = groupDao.getNextGroup()).size() > 0) {
                ObjectNode act = acts.get(0);
                act.put(aggCntName, acts.size());
                double[] sums = new double[aggFields.size()];
                for (int i=0; i<aggFields.size(); i++) {
                    sums[i] += acts.get(i).get(aggFields.get(i)).asDouble();
                }
                for (int i=0; i<aggFields.size(); i++) {
                    act.put(aggFields.get(i) + aggSumAppendix, sums[i]);
                }
                IndexerUtilities.writeOutJson(act, dataFields, writer, separator);
                acts.clear();
            }
            writer.close();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        data.close();
        ObjectNode ret = Json.newObject();
        ret.put(daoNameKey, daoName);
        String path = JsonHelpers.getOptionalString(requestContext.getRequestBody(), filePathKey, filePath);
        ret.set(filesKey, Json.toJson(Lists.newArrayList(path)));
        ret.put(separatorKey, separator);
        return ret;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        indexer.index(documents, requestContext);
    }
}
