package org.grouplens.samantha.server.indexer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.CSVFileDAO;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.featurizer.FeatureExtractorUtilities;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.RetrieverUtilities;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class GroupedIndexer extends AbstractIndexer {
    private final Indexer indexer;
    private final String dataDir;
    private final int numBuckets;
    private final List<String> groupKeys;
    private final List<String> dataFields;
    private final List<String> orderFields;
    private final Boolean descending;
    private final String separator;
    private final String filesKey;
    private final String daoNameKey;
    private final String daoName;
    private final String separatorKey;
    private final int usedBuckets;

    public GroupedIndexer(SamanthaConfigService configService,
                          Configuration config, Injector injector,
                          Configuration daoConfigs, String daoConfigKey,
                          Indexer indexer, String dataDir,
                          int numBuckets, List<String> groupKeys,
                          List<String> dataFields, String separator,
                          List<String> orderFields, Boolean descending,
                          String filesKey, String daoName, String daoNameKey,
                          String separatorKey, int usedBuckets) {
        super(config, configService, daoConfigs, daoConfigKey, injector);
        this.indexer = indexer;
        this.dataDir = dataDir;
        this.numBuckets = numBuckets;
        this.groupKeys = groupKeys;
        this.dataFields = dataFields;
        this.separator = separator;
        this.orderFields = orderFields;
        this.descending = descending;
        this.filesKey = filesKey;
        this.daoName = daoName;
        this.daoNameKey = daoNameKey;
        this.separatorKey = separatorKey;
        this.usedBuckets = usedBuckets;
    }

    public ObjectNode getIndexedDataDAOConfig(RequestContext requestContext) {
        EntityDAO entityDAO = indexer.getEntityDAO(requestContext);
        String prefix = dataDir + "/";
        List<BufferedWriter> writers = new ArrayList<>();
        List<String> files = new ArrayList<>(usedBuckets);
        try {
            new File(prefix).mkdirs();
            for (int i = 0; i < usedBuckets; i++) {
                BufferedWriter writer = new BufferedWriter(new FileWriter(prefix +
                        Integer.valueOf(i).toString() + ".tmp"));
                IndexerUtilities.writeOutHeader(dataFields, writer, separator);
                writers.add(writer);
            }
            while (entityDAO.hasNextEntity()) {
                ObjectNode entity = entityDAO.getNextEntity();
                int idx = FeatureExtractorUtilities.composeConcatenatedKey(entity, groupKeys)
                        .hashCode() % numBuckets;
                if (idx < usedBuckets) {
                    IndexerUtilities.writeOutJson(entity, dataFields, writers.get(idx), separator);
                }
            }
            for (int i = 0; i < usedBuckets; i++) {
                writers.get(i).close();
            }
            writers.clear();
            for (int i=0; i<usedBuckets; i++) {
                String tmpFilePath = prefix + Integer.valueOf(i).toString() + ".tmp";
                File tmpFile = new File(tmpFilePath);
                if (tmpFile.isFile()) {
                    List<ObjectNode> buffer = new ArrayList<>();
                    EntityDAO csvDao = new CSVFileDAO(separator, tmpFilePath);
                    while (csvDao.hasNextEntity()) {
                        buffer.add(csvDao.getNextEntity());
                    }
                    csvDao.close();
                    tmpFile.delete();
                    Comparator<ObjectNode> comparator;
                    if (orderFields == null || orderFields.size() == 0) {
                        comparator = RetrieverUtilities.jsonStringFieldsComparator(groupKeys);
                    } else {
                        List<String> sortFields = new ArrayList<>();
                        sortFields.addAll(groupKeys);
                        sortFields.addAll(orderFields);
                        comparator = RetrieverUtilities.jsonStringFieldsComparator(sortFields);
                        if (descending != null && descending) {
                            comparator = comparator.reversed();
                        }
                    }
                    buffer.sort(comparator);
                    String resultFile = prefix + Integer.valueOf(i).toString() + ".csv";
                    BufferedWriter writer = new BufferedWriter(new FileWriter(resultFile));
                    IndexerUtilities.writeOutHeader(dataFields, writer, separator);
                    for (ObjectNode entity : buffer) {
                        IndexerUtilities.writeOutJson(entity, dataFields, writer, separator);
                    }
                    buffer.clear();
                    writer.close();
                    files.add(resultFile);
                }
            }
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
        entityDAO.close();
        ObjectNode reqDao = Json.newObject();
        reqDao.set(filesKey, Json.toJson(files));
        reqDao.put(separatorKey, separator);
        reqDao.put(daoNameKey, daoName);
        return reqDao;
    }

    public void index(JsonNode documents, RequestContext requestContext) {
        indexer.index(documents, requestContext);
    }
    public void index(RequestContext requestContext) {
        indexer.index(requestContext);
    }
}
