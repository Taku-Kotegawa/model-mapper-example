package com.example.domain.model.test002;

import lombok.Builder;


public record SourceRecord(
        String firstName,
        String lastName,
        String fullFullName
) {
}
