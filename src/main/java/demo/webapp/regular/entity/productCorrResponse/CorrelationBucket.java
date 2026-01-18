package demo.webapp.regular.entity.productCorrResponse;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class CorrelationBucket {
    private double min;
    private double max;
    private int alphas;

    public CorrelationBucket(double min, double max, int alphas) {
        this.min = min;
        this.max = max;
        this.alphas = alphas;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public int getAlphas() {
        return alphas;
    }
}
