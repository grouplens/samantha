/*
 * Copyright (c) [2016-2018] [University of Minnesota]
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

package controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.typesafe.config.ConfigRenderOptions;
import org.grouplens.samantha.server.common.JsonHelpers;
import org.grouplens.samantha.server.config.ConfigKey;
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

    public Result setConfig() {
        JsonNode samantha = request().body().asJson();
        ObjectNode newConfig = Json.newObject();
        newConfig.set(ConfigKey.SAMANTHA_BASE.get(), samantha);
        samanthaConfigService.setConfig(new Configuration(newConfig.toString()));
        ObjectNode resp = JsonHelpers.successJson();
        return ok(resp);
    }

    public Result index() {
        return ok(views.html.Application.index.render());
    }
}
