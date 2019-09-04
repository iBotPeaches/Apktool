package smalinjector.model;

import java.util.Objects;

public class PublicValue {

    public String type;
    public String name;
    public Integer id;

    public PublicValue(Integer id, String name, String type) {
        this.type = type;
        this.name = name;
        this.id = id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PublicValue)) return false;
        PublicValue that = (PublicValue) o;
        return type.equals(that.type) && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(type, name, id);
    }

    @Override
    public String toString() {
        return "PublicValue{" +
                "type='" + type + '\'' +
                ", name='" + name + '\'' +
                ", id='" + id + '\'' +
                '}';
    }
}
