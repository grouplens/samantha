package org.grouplens.samantha.ephemeral;

import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class RandomCSVFileDAO implements EntityDAO {
    private static Logger logger = LoggerFactory.getLogger(RandomCSVFileDAO.class);

    private final CsvSchema schema;
    private final ObjectReader objectReader;
    private final List<CompactCharSequence> lines;
    private int cnt = 0;
    private Random random = new Random();

    private void start() {
        cnt = 0;
    }

    public RandomCSVFileDAO(String separator, String filePath) {
        try {
            CsvSchema.Builder builder = new CsvSchema.Builder();
            builder.setColumnSeparator(separator.charAt(0));

            BufferedReader reader = new BufferedReader(new FileReader(filePath));
            String header = reader.readLine();
            for (String col : header.split(Pattern.quote(separator.substring(0, 1)))) {
                builder.addColumn(col, CsvSchema.ColumnType.NUMBER_OR_STRING);
            }
            this.schema = builder.build();
            this.objectReader = new CsvMapper().readerFor(Map.class).with(schema);

            lines = reader.lines()
                    .map(CompactCharSequence::new)
                    .collect(Collectors.toList());
            reader.close();

            start();
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public boolean hasNextEntity() {
        return cnt < lines.size();
    }

    public ObjectNode getNextEntity() {
        if (!hasNextEntity()) {
            throw new NoSuchElementException();
        }

        String line = lines.get(random.nextInt(lines.size())).toString();
        try {
            ObjectNode entity = Json.newObject();
            IOUtilities.parseEntityFromMap(entity, objectReader.readValue(line));
            cnt++;
            return entity;
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public void restart() {
        start();
    }

    public void close() {}
}