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
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class GenericFilterExpander implements EntityExpander {
    private final String boolFilterKey;
    private final boolean filterIfNotPresent;

    public GenericFilterExpander(String boolFilterKey, boolean filterIfNotPresent) {
        this.boolFilterKey = boolFilterKey;
        this.filterIfNotPresent = filterIfNotPresent;
    }

    private enum CompareRelation implements BoolCompare {
        lt() {
            public boolean compare(JsonNode value1, JsonNode value2) {
                return value1.asDouble() < value2.asDouble();
            }
        },
        gt() {
            public boolean compare(JsonNode value1, JsonNode value2) {
                return value1.asDouble() > value2.asDouble();
            }
        },
        lte() {
            public boolean compare(JsonNode value1, JsonNode value2) {
                return value1.asDouble() <= value2.asDouble();
            }
        },
        gte() {
            public boolean compare(JsonNode value1, JsonNode value2) {
                return value1.asDouble() >= value2.asDouble();
            }
        },
        eq() {
            public boolean compare(JsonNode value1, JsonNode value2) {
                if (value1.isNumber() && value2.isNumber()) {
                    return value1.asDouble() == value2.asDouble();
                } else {
                    return value1.asText().equals(value2.asText());
                }
            }
        };
        CompareRelation() {}
    }

    private interface BoolCompare {
        boolean compare(JsonNode value1, JsonNode value2);
    }

    private boolean evaluateFieldFilter(JsonNode value, JsonNode conditions) {
        if (conditions.isArray()) {
            for (JsonNode inVal : conditions) {
                if (!CompareRelation.eq.compare(value, inVal)) {
                    return false;
                }
            }
        } else if (conditions.isValueNode()) {
            if (!CompareRelation.eq.compare(value, conditions)) {
                return false;
            }
        } else {
            Iterator<String> conds = conditions.fieldNames();
            while (conds.hasNext()) {
                String cond = conds.next();
                if (!CompareRelation.valueOf(cond).compare(value, conditions.get(cond))) {
                    return false;
                }
            }
        }
        return true;
    }

    private boolean evaluateFieldsFilter(JsonNode entity, JsonNode terms) {
        Iterator<String> names = terms.fieldNames();
        while (names.hasNext()) {
            String name = names.next();
            if (entity.has(name)) {
                if (!evaluateFieldFilter(entity.get(name), terms.get(name))) {
                    return false;
                }
            } else if (filterIfNotPresent) {
                return false;
            }
        }
        return true;
    }

    private boolean evaluateFilter(JsonNode entity, JsonNode filter) {
        if (filter.has("bool") && filter.size() == 1) {
            return evaluateBoolFilter(entity, filter.get("bool"));
        } else if (filter.has("term") && filter.size() == 1) {
            return evaluateFieldsFilter(entity, filter.get("term"));
        }
        return true;
    }

    private boolean evaluateComponents(JsonNode entity, JsonNode filter, boolean not, boolean must) {
        if (!filter.isArray()) {
            ArrayNode arr = Json.newArray();
            arr.add(filter);
            filter = arr;
        }
        for (JsonNode one : filter) {
            boolean value = evaluateFilter(entity, one);
            if (!must && value) {
                return true;
            } else if (must) {
                if (!not && !value) {
                    return false;
                } else if (not && value) {
                    return false;
                }
            }
        }
        if (must) {
            return true;
        } else {
            return false;
        }
    }

    private boolean evaluateBoolFilter(JsonNode entity, JsonNode boolFilter) {
        if (boolFilter.has("must")) {
            boolean retain = evaluateComponents(entity, boolFilter.get("must"), false, true);
            if (!retain) {
                return false;
            }
        }
        if (boolFilter.has("must_not")) {
            boolean retain = evaluateComponents(entity, boolFilter.get("must_not"), true, true);
            if (!retain) {
                return false;
            }
        }
        if (boolFilter.has("should")) {
            return evaluateComponents(entity, boolFilter.get("should"), false, false);
        } else {
            return true;
        }
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector,
                                             RequestContext requestContext) {
        Boolean filterIfNotPresent = expanderConfig.getBoolean("filterIfNotPresent");
        if (filterIfNotPresent == null) {
            filterIfNotPresent = true;
        }
        return new GenericFilterExpander(expanderConfig.getString("boolFilterKey"),
                filterIfNotPresent);
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext){
        JsonNode boolFilter = JsonHelpers.getOptionalJson(requestContext.getRequestBody(),
                boolFilterKey);
        List<ObjectNode> filtered = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            if (boolFilter == null || evaluateBoolFilter(entity, boolFilter)) {
                filtered.add(entity);
            }
        }
        return filtered;
    }
}
