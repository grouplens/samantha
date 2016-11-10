package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;
import play.libs.Json;

import java.util.ArrayList;
import java.util.List;

public class JsonStringOrNodeExpander implements EntityExpander {
    private final String jsonAttr;

    private JsonStringOrNodeExpander(String jsonAttr) {
        this.jsonAttr = jsonAttr;
    }

    public static EntityExpander getExpander(Configuration expanderConfig,
                                             Injector injector, RequestContext requestContext) {
        return new JsonStringOrNodeExpander(expanderConfig.getString("jsonAttr"));
    }

    public List<ObjectNode> expand(List<ObjectNode> initialResult,
                                   RequestContext requestContext) {
        List<ObjectNode> expanded = new ArrayList<>();
        for (ObjectNode entity : initialResult) {
            JsonNode json = entity.get(jsonAttr);
            if (json.isTextual()) {
                json = Json.parse(json.asText());
            }
            if (json.isArray()) {
                for (JsonNode one : json) {
                    ObjectNode newEntity = entity.deepCopy();
                    IOUtilities.parseEntityFromJsonNode(one, newEntity);
                    expanded.add(newEntity);
                }
            } else {
                IOUtilities.parseEntityFromJsonNode(json, entity);
                expanded.add(entity);
            }
        }
        return expanded;
    }
}
