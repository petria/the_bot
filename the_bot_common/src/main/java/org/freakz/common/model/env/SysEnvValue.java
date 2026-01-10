package org.freakz.common.model.env;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import org.freakz.common.model.dto.DataNodeBase;

import java.util.Objects;

@JsonIgnoreProperties(ignoreUnknown = true)
public class SysEnvValue extends DataNodeBase {

    @JsonProperty("keyName")
    private String keyName;

    @JsonProperty("value")
    private String value;

    @JsonProperty("modifiedBy")
    private String modifiedBy;

    public SysEnvValue() {
    }

    public SysEnvValue(String keyName, String value, String modifiedBy) {
        this.keyName = keyName;
        this.value = value;
        this.modifiedBy = modifiedBy;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getKeyName() {
        return keyName;
    }

    public void setKeyName(String keyName) {
        this.keyName = keyName;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public String getModifiedBy() {
        return modifiedBy;
    }

    public void setModifiedBy(String modifiedBy) {
        this.modifiedBy = modifiedBy;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SysEnvValue that = (SysEnvValue) o;
        return Objects.equals(keyName, that.keyName) && Objects.equals(value, that.value) && Objects.equals(modifiedBy, that.modifiedBy);
    }

    @Override
    public int hashCode() {
        return Objects.hash(keyName, value, modifiedBy);
    }

    @Override
    public String toString() {
        return "SysEnvValue{" +
                "keyName='" + keyName + '\'' +
                ", value='" + value + '\'' +
                ", modifiedBy='" + modifiedBy + '\'' +
                '}';
    }

    public static class Builder {
        private String keyName;
        private String value;
        private String modifiedBy;

        public Builder keyName(String keyName) {
            this.keyName = keyName;
            return this;
        }

        public Builder value(String value) {
            this.value = value;
            return this;
        }

        public Builder modifiedBy(String modifiedBy) {
            this.modifiedBy = modifiedBy;
            return this;
        }

        public SysEnvValue build() {
            return new SysEnvValue(keyName, value, modifiedBy);
        }
    }
}
