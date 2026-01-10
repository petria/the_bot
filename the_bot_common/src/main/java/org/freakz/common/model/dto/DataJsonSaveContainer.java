package org.freakz.common.model.dto;

import java.util.List;
import java.util.Objects;

public class DataJsonSaveContainer extends DataContainerBase {

    private List<DataNodeBase> data_values;

    public DataJsonSaveContainer() {
    }

    public DataJsonSaveContainer(List<DataNodeBase> data_values) {
        this.data_values = data_values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<DataNodeBase> getData_values() {
        return data_values;
    }

    public void setData_values(List<DataNodeBase> data_values) {
        this.data_values = data_values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        DataJsonSaveContainer that = (DataJsonSaveContainer) o;
        return Objects.equals(data_values, that.data_values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), data_values);
    }

    @Override
    public String toString() {
        return "DataJsonSaveContainer{" +
                "data_values=" + data_values +
                "} " + super.toString();
    }

    public static class Builder {
        private List<DataNodeBase> data_values;

        public Builder data_values(List<DataNodeBase> data_values) {
            this.data_values = data_values;
            return this;
        }

        public DataJsonSaveContainer build() {
            return new DataJsonSaveContainer(data_values);
        }
    }
}
