package com.example.domain.model.test002;

import lombok.Builder;

@Builder
public record DestinationRecord(
        String firstName,
        String lastName,
        String fullFullName
) {
}
