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

package org.grouplens.samantha.server.common;

import com.fasterxml.jackson.databind.JsonNode;
import org.apache.commons.dbutils.QueryRunner;
import org.apache.commons.dbutils.handlers.MapListHandler;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import play.db.DB;
import play.inject.ApplicationLifecycle;
import play.libs.F;
import play.libs.Json;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Singleton
public class JDBCSQLService {
    private static Logger logger = LoggerFactory.getLogger(JDBCSQLService.class);

    @Inject
    private JDBCSQLService(ApplicationLifecycle lifecycle) {
        startUp();
        lifecycle.addStopHook(() -> {
            shutDown();
            return F.Promise.pure(null);
        });
    }

    private void startUp() {}

    private void shutDown() {}

    public List<JsonNode> select(String db, String sql) {
        QueryRunner runner = new QueryRunner(DB.getDataSource(db));
        List<JsonNode> ret = new ArrayList<>();
        try {
            List<Map<String, Object>> results = runner.query(sql, new MapListHandler());
            for (Map<String, Object> result : results) {
                ret.add(Json.toJson(result));
            }
        } catch (SQLException e) {
            throw new BadRequestException(e);
        }
        return ret;
    }

    public int insert(String db, String sql) {
        QueryRunner runner = new QueryRunner(DB.getDataSource(db));
        try {
            int ret = runner.update(sql);
            return ret;
        } catch (SQLException e) {
            throw new BadRequestException(e);
        }
    }
}
