package org.grouplens.samantha.ephemeral;

public class SelectionCriteria {


    public final int n;
    public final String similarityMetric;
    public final String diversityMetric;
    public final double excludeBelow;
    public final int limit;
    public final double ratedDropout;
    public final double dropout;

    public SelectionCriteria(int n, String similarityMetric, String diversityMetric,
                             double excludeBelow, int limit,
                             double ratedDropout, double dropout) {
        this.n = n;
        this.similarityMetric = similarityMetric;
        this.diversityMetric = diversityMetric;
        this.excludeBelow = excludeBelow;
        this.limit = limit;
        this.ratedDropout = ratedDropout;
        this.dropout = dropout;
    }
}
