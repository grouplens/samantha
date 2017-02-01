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

package org.grouplens.samantha.server.router;

import org.grouplens.samantha.server.io.RequestContext;
import play.Configuration;
import play.inject.Injector;

import java.util.List;

public class HashBucketRouterConfig implements RouterConfig {
    private final List<String> predHashAttrs;
    private final List<String> recHashAttrs;
    private final Integer numPredBuckets;
    private final Integer numRecBuckets;
    private final Configuration predName2range;
    private final Configuration recName2range;

    private HashBucketRouterConfig(List<String> predHashAttrs, List<String> recHashAttrs,
                            Integer numPredBuckets, Integer numRecBuckets,
                            Configuration predName2range, Configuration recName2range) {
        this.predHashAttrs = predHashAttrs;
        this.recHashAttrs = recHashAttrs;
        this.numPredBuckets = numPredBuckets;
        this.numRecBuckets = numRecBuckets;
        this.predName2range = predName2range;
        this.recName2range = recName2range;
    }

    public static RouterConfig getRouterConfig(Configuration routerConfig,
                                               Injector injector) {
        Configuration predictorConfig = routerConfig.getConfig("predictorConfig");
        Configuration recommenderConfig = routerConfig.getConfig("recommenderConfig");
        return new HashBucketRouterConfig(
                predictorConfig.getStringList("hashAttrs"),
                recommenderConfig.getStringList("hashAttrs"),
                predictorConfig.getInt("numBuckets"),
                recommenderConfig.getInt("numBuckets"),
                predictorConfig.getConfig("name2range"),
                recommenderConfig.getConfig("name2range")
        );
    }

    public Router getRouter(RequestContext requestContext) {
        return new HashBucketRouter(predHashAttrs, recHashAttrs, numPredBuckets, numRecBuckets,
                predName2range, recName2range);
    }
}
