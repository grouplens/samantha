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

package org.grouplens.samantha.server.scheduler;

import org.grouplens.samantha.server.exception.ConfigurationException;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class QuartzSchedulerService {
    private static Logger logger = LoggerFactory.getLogger(QuartzSchedulerService.class);
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
            logger.error("Scheduling job error: {}", e.getMessage());
        }
    }

    synchronized public void clearAllJobs() {
        try {
            scheduler.clear();
        } catch (SchedulerException e) {
            logger.error("Clearing jobs error: {}", e.getMessage());
        }
    }

    synchronized public void triggerJob(JobKey jobKey) {
        try {
            scheduler.triggerJob(jobKey);
        } catch (SchedulerException e) {
            logger.error("Triggering job {} error: {}", jobKey, e.getMessage());
        }
    }
}
