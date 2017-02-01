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

/**
 * Configuration administration handlers.
 */
public class AdminHandlers extends Controller {
    private final SamanthaConfigService samanthaConfigService;

    /**
     * Constructor of AdminHandler.
     *
     * It is part of the play framework. It will be created (injected) by play framework
     * whenever relevant request urls come.
     *
     * @param samanthaConfigService must be injected with play injector. singleton.
     */
    @Inject
    public AdminHandlers(SamanthaConfigService samanthaConfigService) {
        this.samanthaConfigService = samanthaConfigService;
    }

    /**
     * Handler for reloading the engines configurations.
     *
     * It basically delegates the task the {@link SamanthaConfigService#reloadConfig()}.
     *
     * @return a HTTP response with the key "status" only (mostly "success" if the request is successfully processed).
     */
    public Result reloadConfig() {
        samanthaConfigService.reloadConfig();
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    /**
     * Handler for getting the current configurations of the engines.
     *
     * It basically delegates the task the {@link SamanthaConfigService#getConfig()}. After that, it converts the
     * configuration format into JSON.
     *
     * @return a HTTP response with the keys "status" and "config" where the value of "config" has all configurations
     * of Samantha globally and all the engines.
     */
    public Result getConfig() {
        Configuration config = samanthaConfigService.getConfig();
        JsonNode conf = Json.parse(config.underlying().root().render(ConfigRenderOptions.concise()));
        ObjectNode resp = JsonHelpers.successJson();
        resp.set("config", conf);
        return ok(resp);
    }
}
