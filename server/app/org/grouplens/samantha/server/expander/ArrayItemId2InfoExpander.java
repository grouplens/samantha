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

import java.util.ArrayList;
import java.util.List;

public class ArrayItemId2InfoExpander implements EntityExpander {
    private final String idField;
    private final List<String> infoFields;
    private final List<JsonNode> model;

    public ArrayItemId2InfoExpander(String idField, List<String> infoFields,
                                    List<JsonNode> model) {
        this.idField = idField;
        this.infoFields = infoFields;
        this.model = model;
    }

    static private class ItemId2InfoModelManager extends AbstractModelManager {
        private final Configuration daoConfigs;
        private final String daoConfigKey;
        private final String idField;

        public ItemId2InfoModelManager(Injector injector, String modelName, Configuration daoConfigs,
                                       String daoConfigKey, String idField) {
            super(injector, modelName, null, new ArrayList<>());
            this.daoConfigKey = daoConfigKey;
            this.daoConfigs = daoConfigs;
            this.idField = idField;
        }

        public Object createModel(RequestContext requestContext, SpaceMode spaceMode) {
            return new ArrayList<>();
        }

        public Object buildModel(Object model, RequestContext requestContext) {
            List<JsonNode> info = (List<JsonNode>) model;
            EntityDAO entityDAO = EntityDAOUtilities.getEntityDAO(daoConfigs, requestContext,
                    JsonHelpers.getRequiredJson(requestContext.getRequestBody(), daoConfigKey), injector);
            while (entityDAO.hasNextEntity()) {
                ObjectNode item = entityDAO.getNextEntity();
                int idx = item.get(idField).asInt();
                while (info.size() < idx) {
                    info.add(null);
                }
                info.add(item);
            }
            return info;
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
                expanderConfig.getString("idField"));
        List<JsonNode> model = (List<JsonNode>)manager.manage(requestContext);
        return new ArrayItemId2InfoExpander(expanderConfig.getString("idField"),
                expanderConfig.getStringList("infoFields"), model);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        for (ObjectNode entity : initialResult) {
            if (entity.has(idField)) {
                int idx = entity.get(idField).asInt();
                if (model.size() <= idx || model.get(idx) == null) {
                    Logger.warn("No such item id {} in the item info model with size {}", idx,
                            model.size());
                } else {
                    IOUtilities.parseEntityFromJsonNode(infoFields, model.get(idx), entity);
                }
            } else {
                Logger.error("{} is not present in {}", idField, entity);
            }
        }
        return initialResult;
    }
}
