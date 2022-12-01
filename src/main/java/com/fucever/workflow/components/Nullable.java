package com.fucever.workflow.components;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.function.Consumer;
import java.util.function.Function;

public class Nullable<T> {

    private final T obj;

    private Nullable(T obj) {
        this.obj = obj;
    }

    public static <R> Nullable<R> of(R obj) {
        return new Nullable<>(obj);
    }

    public <K> Nullable<K> get(Function<T, K> function) {
        if (obj == null) {
            return new Nullable<>(null);
        }
        K k = function.apply(obj);
        return new Nullable<>(k);
    }

    public <O> O finalGet(Function<T, O> function) {
        if (obj == null) {
            return null;
        }
        return function.apply(obj);
    }

    public T finalGet() {
        return obj;
    }

    public String finalGetStrDate() {
        if(obj == null){
            return null;
        }
        if (obj instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) obj);
        }
        throw new RuntimeException("不是日期类型");
    }

    public Long finalGetLong(){
        if(obj == null){
            return null;
        }
        return Long.valueOf(obj.toString());
    }

    public BigDecimal finalGetBigDecimal(){
        if(obj == null){
            return null;
        }
        return BigDecimal.valueOf(Double.parseDouble(obj.toString()));
    }

    public String finalGetString(){
        if(obj == null){
            return null;
        }
        return obj.toString();
    }

    public String finalGetString(String defaultValue){
        if(obj == null){
            return defaultValue;
        }
        return obj.toString();
    }

    public Byte finalGetByte(){
        if(obj == null){
            return null;
        }
        return Byte.valueOf(obj.toString());
    }

    public void process(Consumer<T> function) {
        if (obj != null) {
            function.accept(obj);
        }
    }
}
