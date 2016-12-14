package org.grouplens.samantha.server.config;

import org.grouplens.samantha.server.io.RequestContext;

interface ComponentGetter {
    void getComponent(SamanthaConfigService configService, String componentName, RequestContext requestContext);
}
