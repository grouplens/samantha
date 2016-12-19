package org.grouplens.samantha.server.common;

import play.api.mvc.EssentialFilter;
import play.http.HttpFilters;
import javax.inject.Inject;

public class Filters implements HttpFilters {
    private final LoggingFilter loggingFilter;

    @Inject
    public Filters(LoggingFilter loggingFilter) {
        this.loggingFilter = loggingFilter;
    }

    @Override
    public EssentialFilter[] filters() {
        return new EssentialFilter[] {loggingFilter};
    }
}