package org.grouplens.samantha.server.scheduler;

import org.grouplens.samantha.server.exception.ConfigurationException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuartzSchedulerService {
    private final Scheduler scheduler;

    @Inject
    private QuartzSchedulerService() {
        try {
            scheduler = StdSchedulerFactory.getDefaultScheduler();
            scheduler.start();
        } catch (SchedulerException e) {
            throw new ConfigurationException(e);
        }
    }

    synchronized public void scheduleJob(Trigger trigger, JobDetail job) {
        try {
            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new ConfigurationException(e);
        }
    }

    synchronized public void clearAllJobs() {
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            throw new ConfigurationException(e);
        }
    }
}
