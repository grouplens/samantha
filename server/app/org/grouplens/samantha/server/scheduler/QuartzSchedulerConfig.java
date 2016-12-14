package org.grouplens.samantha.server.scheduler;

import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.exception.ConfigurationException;
import org.quartz.*;
import play.Configuration;
import play.inject.Injector;

import java.text.ParseException;

import static org.quartz.CronScheduleBuilder.cronSchedule;
import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

public class QuartzSchedulerConfig implements SchedulerConfig {
    private final Configuration config;
    private final Injector injector;
    private final String engineName;

    private QuartzSchedulerConfig(Injector injector, Configuration config, String engineName) {
        this.config = config;
        this.injector = injector;
        this.engineName = engineName;
    }

    public static SchedulerConfig getSchedulerConfig(String engineName,
                                                     Configuration schedulerConfig,
                                                     Injector injector) {
        return new QuartzSchedulerConfig(injector, schedulerConfig, engineName);
    }

    public void scheduleJobs() {
        String cronExprStr = config.getString("cronExpression");
        CronExpression cronExpr;
        try {
            cronExpr = new CronExpression(cronExprStr);
        } catch (ParseException e) {
            throw new ConfigurationException(e);
        }
        String name = config.getString(ConfigKey.ENGINE_COMPONENT_NAME.get());
        Trigger trigger = newTrigger().withIdentity(name).withSchedule(cronSchedule(cronExpr)).build();
        Configuration jobConfig = config.getConfig("jobConfig");
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put("jobConfig", jobConfig);
        jobDataMap.put("injector", injector);
        jobDataMap.put("engineName", engineName);
        String jobClass = config.getString("jobClass");
        try {
            JobDetail jobDetail = newJob(Class.forName(jobClass).asSubclass(Job.class))
                    .withIdentity(name).usingJobData(jobDataMap).build();
            QuartzSchedulerService schedulerService = injector.instanceOf(QuartzSchedulerService.class);
            schedulerService.scheduleJob(trigger, jobDetail);
        } catch (ClassNotFoundException e) {
            throw new ConfigurationException(e);
        }
    }
}
