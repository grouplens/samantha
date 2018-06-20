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

package org.grouplens.samantha.server.xgboost;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.modeler.xgboost.XGBoostModel;
import org.grouplens.samantha.server.expander.EntityExpander;
import org.grouplens.samantha.server.expander.ExpanderUtilities;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.retriever.AbstractRetriever;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import play.Configuration;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class XGBoostClassifierRetriever extends AbstractRetriever {
    private final XGBoostModel model;
    private final List<EntityExpander> expanders;

    public XGBoostClassifierRetriever(XGBoostModel model, List<EntityExpander> expanders, Configuration config) {
        super(config);
        this.model = model;
        this.expanders = expanders;
    }

    public RetrievedResult retrieve(RequestContext requestContext) {
        ObjectNode entity = Json.newObject();
        IOUtilities.parseEntityFromJsonNode(requestContext.getRequestBody(), entity);
        List<ObjectNode> entities = new ArrayList<>();
        entities.add(entity);
        entities = ExpanderUtilities.expand(entities, expanders, requestContext);
        List<ObjectNode> results = model.classify(entities);
        return new RetrievedResult(results, results.size());
    }
}
