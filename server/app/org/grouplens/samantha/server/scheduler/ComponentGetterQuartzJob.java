package org.grouplens.samantha.server.scheduler;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
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
import play.inject.Injector;
import play.libs.Json;

public class ComponentGetterQuartzJob implements Job {

    public void execute(JobExecutionContext context) {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String engineName = dataMap.getString("engineName");
        Injector injector = (Injector) dataMap.get("injector");
        SamanthaConfigService configService = injector.instanceOf(SamanthaConfigService.class);
        Configuration jobConfig = (Configuration) dataMap.get("jobConfig");
        for (Configuration taskConfig: jobConfig.getConfigList("tasks")) {
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
            EngineComponent.valueOf(type).getComponent(configService, name, pseudoRequest);
        }
    }
}
