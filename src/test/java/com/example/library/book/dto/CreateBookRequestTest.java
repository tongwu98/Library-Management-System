package com.example.library.book.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;

class CreateBookRequestTest {

	private static ValidatorFactory factory;

	private static Validator validator;

	@BeforeAll
	static void setUp() {
		factory = Validation.buildDefaultValidatorFactory();
		validator = factory.getValidator();
	}

	@AfterAll
	static void tearDown() {
		factory.close();
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	void givenBlankTitle_whenValidatingRequest_thenReportsTitleError(String title) {
		assertThat(validator.validate(new CreateBookRequest(title, "Frank Herbert"))).singleElement()
			.satisfies(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("title"));
	}

	@ParameterizedTest
	@NullAndEmptySource
	@ValueSource(strings = { " ", "\t\n" })
	void givenBlankAuthor_whenValidatingRequest_thenReportsAuthorError(String author) {
		assertThat(validator.validate(new CreateBookRequest("Dune", author))).singleElement()
			.satisfies(error -> assertThat(error.getPropertyPath().toString()).isEqualTo("author"));
	}

	@ParameterizedTest
	@ValueSource(strings = { "Dune", "Frank Herbert", "  Dune  ", "A\nTitle" })
	void givenNonblankDetails_whenValidatingRequest_thenAcceptsRequest(String value) {
		assertThat(validator.validate(new CreateBookRequest(value, value))).isEmpty();
	}

}
