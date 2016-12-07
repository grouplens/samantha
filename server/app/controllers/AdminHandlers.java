package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
import play.Configuration;
import play.libs.Json;
import play.mvc.Controller;
import play.mvc.Result;

import javax.inject.Inject;

public class AdminHandlers extends Controller {
    private final SamanthaConfigService samanthaConfigService;

    @Inject
    public AdminHandlers(SamanthaConfigService samanthaConfigService) {
        this.samanthaConfigService = samanthaConfigService;
    }

    public Result reloadConfig() {
        samanthaConfigService.reloadConfig();
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    public Result getConfig() {
        ObjectNode resp = JsonHelpers.successJson();
        Configuration config = samanthaConfigService.getConfig();
        JsonNode conf = Json.parse(config.underlying().root().render(ConfigRenderOptions.concise()));
        resp.set("config", conf);
        return ok(resp);
    }
}
