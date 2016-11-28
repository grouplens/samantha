package controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.SamanthaConfigService;
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
}
