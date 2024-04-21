package com.example.config;

import org.modelmapper.ModelMapper;

import java.util.Map;
import java.util.Objects;

public class ModelMapperHolder {

    private final Map<String, ModelMapper> modelMappers;

    ModelMapperHolder(Map<String, ModelMapper> modelMappers) {
        this.modelMappers = modelMappers;
    }

    public ModelMapper get(String key) {
        Objects.requireNonNull(key);
        var mm = modelMappers.get(key);
        if (mm == null) {
            throw new IllegalStateException("ModelMapper not created. (" + key + ")");
        }
        return mm;
    }

}
