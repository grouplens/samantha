package org.grouplens.samantha.server.scheduler;

import play.Configuration;
import play.inject.Injector;

//TODO: enable schedule multiple different types of jobs in one schedule
public interface SchedulerConfig {
    static SchedulerConfig getSchedulerConfig(String engineName,
                                   Configuration schedulerConfig,
                                   Injector injector) {return null;}
    void scheduleJobs();
}
