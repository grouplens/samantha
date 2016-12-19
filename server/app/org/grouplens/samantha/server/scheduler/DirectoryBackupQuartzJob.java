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
                String[] fields = reserveStr.split(" ");
                Date cutDate = new Date(now.getTime() - TimeUnit
                        .valueOf(fields[1]).toMillis(Long.parseLong(fields[0])));
                File[] prevDirs = new File(destDir).listFiles(File::isDirectory);
                for (File dir : prevDirs) {
                    try {
                        Date curDate = format.parse(dir.getName());
                        if (curDate.before(cutDate)) {
                            dir.delete();
                        }
                    } catch (ParseException e) {
                        Logger.error(e.getMessage());
                    }
                }
            }
        }
    }
}
