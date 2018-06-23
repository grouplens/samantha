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

package org.grouplens.samantha.server.recommender;

import org.grouplens.samantha.server.exception.BadRequestException;
import org.grouplens.samantha.server.io.RequestContext;
import org.grouplens.samantha.server.ranker.RankedResult;
import org.grouplens.samantha.server.ranker.Ranker;
import org.grouplens.samantha.server.retriever.RetrievedResult;
import org.grouplens.samantha.server.retriever.Retriever;
import play.Configuration;
import play.Logger;

public class StandardRecommender implements Recommender {
    private final Configuration config;
    private final Retriever retriever;
    private final Ranker ranker;

    public StandardRecommender(Configuration config, Retriever retriever, Ranker ranker) {
        this.retriever = retriever;
        this.ranker = ranker;
        this.config = config;
    }

    public RankedResult recommend(RequestContext requestContext)
            throws BadRequestException {
        long start = System.currentTimeMillis();
        RetrievedResult retrievedResult = retriever.retrieve(requestContext);
        Logger.debug("Retriever time: {}", System.currentTimeMillis() - start);
        start = System.currentTimeMillis();
        RankedResult recommendations = ranker.rank(retrievedResult, requestContext);
        Logger.debug("Ranker time: {}", System.currentTimeMillis() - start);
        return recommendations;
    }

    public Configuration getConfig() {
        return config;
    }
}
