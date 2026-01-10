package org.freakz.common.model.dto;

import java.util.List;
import java.util.Objects;

public class DataValuesJsonContainer extends DataContainerBase {

    private List<DataValues> data_values;

    public DataValuesJsonContainer() {
    }

    public DataValuesJsonContainer(List<DataValues> data_values) {
        this.data_values = data_values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<DataValues> getData_values() {
        return data_values;
    }

    public void setData_values(List<DataValues> data_values) {
        this.data_values = data_values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DataValuesJsonContainer that = (DataValuesJsonContainer) o;
        return Objects.equals(data_values, that.data_values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data_values);
    }

    @Override
    public String toString() {
        return "DataValuesJsonContainer{" +
                "data_values=" + data_values +
                '}';
    }

    public static class Builder {
        private List<DataValues> data_values;

        public Builder data_values(List<DataValues> data_values) {
            this.data_values = data_values;
            return this;
        }

        public DataValuesJsonContainer build() {
            return new DataValuesJsonContainer(data_values);
        }
    }
}
