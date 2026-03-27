package com.example.holdem.table.presentation.rest.dto;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

class CreateTableRequestValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void maxPlayersCannotExceedEleven() {
        CreateTableRequest request = new CreateTableRequest("full-ring-plus", 12, 50, 100);

        var violations = validator.validate(request);

        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getPropertyPath().toString()).isEqualTo("maxPlayers");
    }
}
