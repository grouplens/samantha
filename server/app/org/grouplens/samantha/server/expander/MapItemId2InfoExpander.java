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

package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.server.common.AbstractModelManager;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MapItemId2InfoExpander implements EntityExpander {
    private final String idField;
    private final List<String> infoFields;
    private final Map<String, JsonNode> model;

    public MapItemId2InfoExpander(String idField, List<String> infoFields,
                                  Map<String, JsonNode> model) {
        this.idField = idField;
        this.infoFields = infoFields;
        this.model = model;
    }

    static private class ItemId2InfoModelManager extends AbstractModelManager {
        private final Configuration daoConfigs;
        private final String daoConfigKey;
        private final String idField;
        private final List<String> infoFields;

        public ItemId2InfoModelManager(Injector injector, String modelName, Configuration daoConfigs,
                                       String daoConfigKey, String idField, List<String> infoFields) {
            super(injector, modelName, null, new ArrayList<>());
            this.daoConfigKey = daoConfigKey;
            this.daoConfigs = daoConfigs;
            this.idField = idField;
            this.infoFields = infoFields;
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            return new HashMap<>();
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            Map<String, JsonNode> item2info = (Map<String, JsonNode>) model;
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    JsonHelpers.getRequiredJson(requestContext.getRequestBody(), daoConfigKey), injector);
            while (entityDAO.hasNextEntity()) {
                JsonNode item = entityDAO.getNextEntity();
                ObjectNode info = Json.newObject();
                IOUtilities.parseEntityFromJsonNode(infoFields, item, info);
                String key = item.get(idField).asText();
                item2info.put(key, info);
            }
            return item2info;
        }

        public Object dumpModel(RequestContext requestContext) {
            throw new BadRequestException("Dumping model is not supported in this expander.");
        }
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        Configuration daoConfigs = expanderConfig.getConfig(ConfigKey.ENTITY_DAOS_CONFIG.get());
        ItemId2InfoModelManager manager = new ItemId2InfoModelManager(injector,
                expanderConfig.getString("modelName"), daoConfigs,
                expanderConfig.getString("daoConfigKey"),
                expanderConfig.getString("idField"),
                expanderConfig.getStringList("infoFields"));
        Map<String, JsonNode> model = (Map<String, JsonNode>)manager.manage(requestContext);
        return new MapItemId2InfoExpander(expanderConfig.getString("idField"),
                expanderConfig.getStringList("infoFields"), model);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            if (entity.has(idField)) {
                String key = entity.get(idField).asText();
                if (model.containsKey(key)) {
                    IOUtilities.parseEntityFromJsonNode(infoFields, model.get(key), entity);
                } else {
                    Logger.warn("{} is not present in item2info with item {} and infos {}.",
                            key, idField, infoFields);
                }
            } else {
                Logger.warn("{} is not present in {}", idField, entity);
            }
        }
        return initialResult;
    }
}
