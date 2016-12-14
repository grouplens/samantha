package org.grouplens.samantha.server.common;

import org.grouplens.samantha.server.expander.ExpanderUtilities;
import play.Configuration;

import java.util.List;

abstract public class AbstractComponentConfig {
    final protected Configuration config;
    final protected List<Configuration> expandersConfig;

    public AbstractComponentConfig(Configuration config) {
        this.config = config;
        this.expandersConfig = ExpanderUtilities.getEntityExpandersConfig(config);
    }

    public Configuration getConfig() {
        return config;
    }
}
