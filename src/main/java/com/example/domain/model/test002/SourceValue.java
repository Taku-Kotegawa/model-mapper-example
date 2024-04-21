package com.example.domain.model.test002;

import lombok.Builder;
import lombok.Value;

@Builder
@Value
public final class SourceValue {
    private final String firstName;
    private final String lastName;
    private final String nullField;
    private final String fullName;
    private final String ignoreField;
}
