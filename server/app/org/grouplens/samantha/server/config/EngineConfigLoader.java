package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.exception.ConfigurationException;
import play.Configuration;
import play.inject.Injector;

interface EngineConfigLoader {
    EngineConfig loadConfig(String engineName, Configuration engineConfig, Injector injector)
            throws ConfigurationException;
}
