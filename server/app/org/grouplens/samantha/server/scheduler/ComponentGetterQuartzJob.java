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

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.server.common.Utilities;
import org.grouplens.samantha.server.config.ConfigKey;
import org.grouplens.samantha.server.config.EngineComponent;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import org.grouplens.samantha.server.indexer.Indexer;
import org.grouplens.samantha.server.io.IOUtilities;
import org.grouplens.samantha.server.io.RequestContext;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import play.Configuration;
import play.Logger;
import play.inject.Injector;
import play.libs.Json;

import java.util.List;

public class ComponentGetterQuartzJob implements Job {

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String engineName = dataMap.getString("engineName");
        Injector injector = (Injector) dataMap.get("injector");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Configuration jobConfig = (Configuration) dataMap.get("jobConfig");
        for (Configuration taskConfig: jobConfig.getConfigList("tasks")) {
            List<String> hosts = taskConfig.getStringList("hosts");
            if (hosts != null && !Utilities.isInHosts(hosts)) {
                continue;
            }
            ObjectNode reqBody = Json.newObject();
            for (Configuration indexedData : taskConfig.getConfigList("indexerData")) {
                RequestContext pseudoRequest = new RequestContext(
                        Json.parse(indexedData.getConfig(ConfigKey.REQUEST_CONTEXT.get())
                                .underlying().root().render(ConfigRenderOptions.concise())),
                        engineName);
                Indexer indexer = configService.getIndexer(indexedData.getString("indexerName"), pseudoRequest);
                reqBody.set(indexedData.getString("daoConfigKey"),
                        indexer.getIndexedDataDAOConfig(pseudoRequest));
            }
            for (Configuration otherData : taskConfig.getConfigList("otherData")) {
                reqBody.set(otherData.getString("daoConfigKey"),
                        Json.parse(otherData.getConfig("daoConfig").underlying().root()
                        .render(ConfigRenderOptions.concise())));
            }
            Configuration runnerConfig = taskConfig.getConfig("runner");
            IOUtilities.parseEntityFromJsonNode(
                    Json.parse(runnerConfig.getConfig(ConfigKey.REQUEST_CONTEXT.get())
                            .underlying().root().render(ConfigRenderOptions.concise())), reqBody);
            RequestContext pseudoRequest = new RequestContext(reqBody, engineName);
            String name = runnerConfig.getString(ConfigKey.ENGINE_COMPONENT_NAME.get());
            String type = runnerConfig.getString(ConfigKey.ENGINE_COMPONENT_TYPE.get());
            ObjectNode logInfo = Json.newObject();
            logInfo.put(ConfigKey.ENGINE_NAME.get(), engineName);
            logInfo.put(ConfigKey.ENGINE_COMPONENT_NAME.get(), name);
            logInfo.put(ConfigKey.ENGINE_COMPONENT_TYPE.get(), type);
            logInfo.set(ConfigKey.REQUEST_CONTEXT.get(), reqBody);
            Logger.info(logInfo.toString());
            EngineComponent.valueOf(type).getComponent(configService, name, pseudoRequest);
        }
    }
}
