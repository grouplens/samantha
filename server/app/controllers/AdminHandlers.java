package controllers;

import play.mvc.Controller;
import play.mvc.Result;

public class AdminHandlers extends Controller {

    public Result reloadConfig() {
        return badRequest("Hot reloading of configurations is not supported, yet!");
    }
}
