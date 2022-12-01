package com.fucever.workflow.dto;

import java.util.Collection;
import java.util.function.Supplier;

public interface LoginUser {
    public Collection<String> getAuthority();
    public String user();
}
