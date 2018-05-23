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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.dao.EntityDAO;
import org.grouplens.samantha.modeler.model.SpaceMode;
import org.grouplens.samantha.server.dao.EntityDAOUtilities;
import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.ArrayList;
import java.util.List;

public class ItemId2InfoModelManager extends AbstractModelManager {
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
