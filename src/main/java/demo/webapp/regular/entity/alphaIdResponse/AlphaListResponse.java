package demo.webapp.regular.entity.alphaIdResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphaListResponse {
    private int count;
    private String next;
    private String previous;
    private List<AlphaItem> results;

    // getters & setters
    public int getCount() {
        return count;
    }

    public void setCount(int count) {
        this.count = count;
    }

    public List<AlphaItem> getResults() {
        return results;
    }

    public void setResults(List<AlphaItem> results) {
        this.results = results;
    }
}
