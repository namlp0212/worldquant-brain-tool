package demo.webapp.regular.entity.productCorrResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class CorrelationResponse {
    private Schema schema;
    private List<List<Number>> records;
    private double max;
    private double min;

    // getters & setters
    public Schema getSchema() {
        return schema;
    }

    public void setSchema(Schema schema) {
        this.schema = schema;
    }

    public List<List<Number>> getRecords() {
        return records;
    }

    public void setRecords(List<List<Number>> records) {
        this.records = records;
    }

    public double getMax() {
        return max;
    }

    public void setMax(double max) {
        this.max = max;
    }

    public double getMin() {
        return min;
    }

    public void setMin(double min) {
        this.min = min;
    }
}
