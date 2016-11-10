package org.grouplens.samantha.server.expander;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public interface EntityExpander {
    static EntityExpander getExpander(Configuration expanderConfig,
                                      Injector injector, RequestContext requestContext) {return null;}
    List<ObjectNode> expand(List<ObjectNode> initialResult,
                           RequestContext requestContext);
}
