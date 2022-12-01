package com.fucever.workflow.components;

import org.apache.commons.codec.binary.Hex;

import java.nio.charset.StandardCharsets;

public class HexConvertor {
    public static String toHexString(byte[] ba) {
        return Hex.encodeHexString(ba);
    }

    public static String toHexString(String ba){
        return Hex.encodeHexString(ba.getBytes(StandardCharsets.UTF_8));
    }
}
