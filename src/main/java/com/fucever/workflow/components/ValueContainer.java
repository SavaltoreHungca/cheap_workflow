package com.fucever.workflow.components;

public class ValueContainer<T> {
    private T value;

    public ValueContainer() {
    }

    public ValueContainer(T value) {
        this.value = value;
    }

    public T get() {
        return value;
    }

    public void set(T value) {
        this.value = value;
    }

    public boolean isNull(){
        return this.value == null;
    }
}
