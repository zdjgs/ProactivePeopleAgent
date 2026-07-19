package com.proactiveperson.memory.mem0;

import com.proactiveperson.common.domain.MemoryLayer;

/**
 * 用 Mem0 metadata 映射本项目的短/中/长三层语义。
 */
public final class Mem0LayerMetadata {

    public static final String LAYER_KEY = "pp_layer";
    public static final String PREF_KEY = "pp_pref_key";

    private Mem0LayerMetadata() {
    }

    public static String layerValue(MemoryLayer layer) {
        return layer.name();
    }
}
