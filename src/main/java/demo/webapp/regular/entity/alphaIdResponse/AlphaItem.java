package demo.webapp.regular.entity.alphaIdResponse;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonRawValue;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public class AlphaItem {
    private String id;

    // giữ raw JSON cho các block lớn
    @JsonRawValue
    private String settings;

    @JsonRawValue
    private String regular;

    // Use JsonNode so Jackson can deserialize the nested object directly
    private JsonNode is;

    @JsonRawValue
    private String classifications;

    // getters & setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getIs() {
        return is != null ? is.toString() : null;
    }

    public void setIs(JsonNode is) {
        this.is = is;
    }
}
