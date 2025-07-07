/*
 * Copyright 2012-present the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.web.error;

import java.util.List;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

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
	void errorCauseDoesNotAppearInJson() throws JsonProcessingException {
		String json = new ObjectMapper()
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
