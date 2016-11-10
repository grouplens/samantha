package org.grouplens.samantha.server.reinforce;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public interface Transitioner {
    static Transitioner getTransitioner(Configuration config,
                                        Injector injector, RequestContext requestContext) {return null;}
    /**
     * Set the current state and set the probability of each new state in the attribute of the
     * returned ObjectNode with given key in ConfigKey
     */
    List<ObjectNode> transition(ObjectNode currentState, ObjectNode action);
}
