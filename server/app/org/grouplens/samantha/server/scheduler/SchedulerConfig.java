package org.grouplens.samantha.server.scheduler;

import play.Configuration;
import play.inject.Injector;

public interface SchedulerConfig {
    static void getSchedulerConfig(String engineName,
                                   Configuration schedulerConfig,
                                   Injector injector) {}
    void scheduleJobs();
}
