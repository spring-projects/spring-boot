/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.web.error;

import java.util.List;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link Error}.
 *
 * @author Andy Wilkinson
 */
class ErrorTests {

	@Test
	@SuppressWarnings("rawtypes")
	void wrapIfNecessaryDoesNotWrapFieldErrorOrObjectError() {
		List<MessageSourceResolvable> wrapped = Error.wrapIfNecessary(List.of(new ObjectError("name", "message"),
				new FieldError("name", "field", "message"), new CustomMessageSourceResolvable("code")));
		assertThat(wrapped).extracting((error) -> (Class) error.getClass())
			.containsExactly(ObjectError.class, FieldError.class, Error.class);
	}

	@Test
	void errorCauseDoesNotAppearInJson() {
		String json = new JsonMapper()
			.writeValueAsString(Error.wrapIfNecessary(List.of(new CustomMessageSourceResolvable("code"))));
		assertThat(json).doesNotContain("some detail");
	}

	public static class CustomMessageSourceResolvable extends DefaultMessageSourceResolvable {

		CustomMessageSourceResolvable(String code) {
			super(code);
		}

		public String getDetail() {
			return "some detail";
		}

	}

}
