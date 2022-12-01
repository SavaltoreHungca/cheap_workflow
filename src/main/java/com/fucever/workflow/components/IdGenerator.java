package com.fucever.workflow.components;

import java.util.UUID;

public class IdGenerator {

    public static Long genId() {
        int userId = UUID.randomUUID().hashCode();
        userId = userId < 0 ? -userId : userId;
        return Long.valueOf(String.valueOf(userId));
    }

}
