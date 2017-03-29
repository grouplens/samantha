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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.libs.Json;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class JsonFileDAO implements EntityDAO {
    private static Logger logger = LoggerFactory.getLogger(JsonFileDAO.class);
    private final String filePath;
    private BufferedReader reader;
    private String line;
    private int cnt = 0;

    private void start() {
        try {
            this.reader = new BufferedReader(new FileReader(this.filePath));
            this.cnt = 0;
            this.line = null;
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
    }

    public JsonFileDAO(String filePath) {
        this.filePath = filePath;
        start();
    }

    public boolean hasNextEntity() {
        if (reader == null) {
            return false;
        }
        if (line != null) {
            return true;
        }
        try {
            line = reader.readLine();
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
        if (line == null) {
            return false;
        } else {
            return true;
        }
    }

    public ObjectNode getNextEntity() {
        if (line == null) {
            try {
                line = reader.readLine();
            } catch (IOException e) {
                logger.error("{}", e.getMessage());
                throw new BadRequestException(e);
            }
        }
        ObjectNode entity = Json.newObject();
        JsonNode data = Json.parse(line);
        line = null;
        IOUtilities.parseEntityFromJsonNode(data, entity);
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
            reader = null;
            line = null;
            cnt = 0;
        } catch (IOException e) {
            logger.error("{}", e.getMessage());
            throw new BadRequestException(e);
        }
    }
}
