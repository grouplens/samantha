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

import com.typesafe.config.ConfigRenderOptions;
import org.apache.commons.io.FileUtils;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import play.Configuration;
import play.Logger;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class DirectoryBackupQuartzJob implements Job {

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        Configuration jobConfig = (Configuration) dataMap.get("jobConfig");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
        for (Configuration taskConfig : jobConfig.getConfigList("tasks")) {
            String sourceDir = taskConfig.getString("sourceDir");
            String destDir = taskConfig.getString("destDir");
            Date now = new Date();
            String dateStr = format.format(now);
            Logger.info("Directory backup job is working: {}",
                    taskConfig.underlying().root().render(ConfigRenderOptions.concise()));
            try {
                File target = new File(destDir + "/" + dateStr);
                FileUtils.copyDirectory(new File(sourceDir), target);
            } catch (IOException e) {
                Logger.error(e.getMessage());
            }
            String reserveStr = taskConfig.getString("reserve");
            if (reserveStr != null) {
                String[] fields = reserveStr.split(" ", -1);
                Date cutDate = new Date(now.getTime() - TimeUnit
                        .valueOf(fields[1]).toMillis(Long.parseLong(fields[0])));
                File[] prevDirs = new File(destDir).listFiles(File::isDirectory);
                for (File dir : prevDirs) {
                    try {
                        Date curDate = format.parse(dir.getName());
                        if (curDate.before(cutDate)) {
                            FileUtils.deleteDirectory(dir);
                        }
                    } catch (IOException | ParseException e) {
                        Logger.error(e.getMessage());
                    }
                }
            }
        }
    }
}
