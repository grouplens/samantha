package org.grouplens.samantha.ephemeral;

public class SelectionCriteria {
    public final int limit;
    public final int n;
    public final double ratedDropout;
//    public final double itemBiasWeighting;
//    public final double logSupportWeighting;
//    public final double avgRatingWeighting;
//    public final double logNumRatingsWeighting;
//    public final double halflife15YearsWeighting;
    public final double dropout;

    public SelectionCriteria(int limit, int n,
                             double ratedDropout,
//                             double itemBiasWeighting,
//                             double logSupportWeighting,
//                             double avgRatingWeighting,
//                             double logNumRatingsWeighting,
//                             double halflife15YearsWeighting,
                             double dropout) {
        this.limit = limit;
        this.n = n;
        this.ratedDropout = ratedDropout;
//        this.itemBiasWeighting = itemBiasWeighting;
//        this.logSupportWeighting = logSupportWeighting;
//        this.avgRatingWeighting = avgRatingWeighting;
//        this.logNumRatingsWeighting = logNumRatingsWeighting;
//        this.halflife15YearsWeighting = halflife15YearsWeighting;
        this.dropout = dropout;
    }
}
