package org.freakz.common.model.dto;

import org.freakz.common.model.users.User;

import java.util.List;
import java.util.Objects;

public class UserValuesJsonContainer extends DataContainerBase {

    private List<User> data_values;

    public UserValuesJsonContainer() {
    }

    public UserValuesJsonContainer(List<User> data_values) {
        this.data_values = data_values;
    }

    public static Builder builder() {
        return new Builder();
    }

    public List<User> getData_values() {
        return data_values;
    }

    public void setData_values(List<User> data_values) {
        this.data_values = data_values;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        UserValuesJsonContainer that = (UserValuesJsonContainer) o;
        return Objects.equals(data_values, that.data_values);
    }

    @Override
    public int hashCode() {
        return Objects.hash(data_values);
    }

    @Override
    public String toString() {
        return "UserValuesJsonContainer{" +
                "data_values=" + data_values +
                '}';
    }

    public static class Builder {
        private List<User> data_values;

        public Builder data_values(List<User> data_values) {
            this.data_values = data_values;
            return this;
        }

        public UserValuesJsonContainer build() {
            return new UserValuesJsonContainer(data_values);
        }
    }
}
