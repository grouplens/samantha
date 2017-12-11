/*
 * Copyright (c) [2016-2017] [University of Minnesota]
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package org.grouplens.samantha.modeler.dao;

import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.dataformat.csv.CsvMapper;
import com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

public class CSVFileDAO implements EntityDAO {
    private static Logger logger = LoggerFactory.getLogger(CSVFileDAO.class);
    private final String filePath;
    private BufferedReader reader;
    private MappingIterator<Map<String, Object>> it;
    private final CsvMapper mapper = new CsvMapper();
    private final CsvSchema schema;
    private int cnt = 0;

    private void start() {
        try {
            this.reader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(filePath), StandardCharsets.UTF_8));
            this.it = this.mapper.readerFor(Map.class)
                    .with(this.schema)
                    .readValues(this.reader);
            this.cnt = 0;
        } catch (IOException e) {
            throw new BadRequestException(e);
        }
    }

    public CSVFileDAO(String separator, String filePath) {
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
        Map<String, Object> rowAsMap;
        try {
            rowAsMap = it.next();
        } catch (Exception e) {
            rowAsMap = new HashMap<>();
        }
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
            throw new BadRequestException(e);
        }
    }
}
