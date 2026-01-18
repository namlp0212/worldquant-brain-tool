package demo.webapp.regular.entity.alphaIdResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphaItem {
    private String id;

    // giữ raw JSON cho các block lớn
    @JsonRawValue
    private String settings;

    @JsonRawValue
    private String regular;

    @JsonRawValue
    private String is;

    @JsonRawValue
    private String classifications;

    // getters & setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
