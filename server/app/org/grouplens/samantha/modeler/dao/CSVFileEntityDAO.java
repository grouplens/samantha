package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.grouplens.samantha.server.exception.InvalidRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Map;

public class CSVFileEntityDAO implements EntityDAO {
    private static Logger logger = LoggerFactory.getLogger(CSVFileEntityDAO.class);
    private final String filePath;
    private BufferedReader reader;
    private MappingIterator<Map<String, Object>> it;
    private final CsvMapper mapper = new CsvMapper();
    private final CsvSchema schema;
    private int cnt = 0;

    private void start() {
        try {
            this.reader = new BufferedReader(new FileReader(this.filePath));
            this.it = this.mapper.readerFor(Map.class)
                    .with(this.schema)
                    .readValues(this.reader);
            this.cnt = 0;
        } catch (IOException e) {
            throw new InvalidRequestException(e);
        }
    }

    public CSVFileEntityDAO(String separator, String filePath) {
        this.filePath = filePath;
        this.schema = CsvSchema.emptySchema().withHeader().withColumnSeparator(separator.charAt(0));
        start();
    }

    public boolean hasNextEntity() {
        if (it == null) {
            return false;
        }
        return it.hasNext();
    }

    public ObjectNode getNextEntity() {
        Map<String, Object> rowAsMap = it.next();
        ObjectNode entity = Json.newObject();
        IOUtilities.parseEntityFromMap(entity, rowAsMap);
        cnt++;
        if (cnt % 1000000 == 0) {
            logger.info("Read {} entities.", cnt);
        }
        return entity;
    }

    public void restart() {
        start();
    }

    public void close() {
        if (reader == null) {
            return;
        }
        try {
            reader.close();
            it.close();
            reader = null;
            it = null;
        } catch (IOException e) {
            throw new InvalidRequestException(e);
        }
    }
}
