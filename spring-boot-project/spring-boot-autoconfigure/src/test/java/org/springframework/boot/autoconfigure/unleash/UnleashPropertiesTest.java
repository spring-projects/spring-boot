/*
 * Copyright 2022 the original author or authors.
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

package org.springframework.boot.autoconfigure.unleash;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.function.Function;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link UnleashProperties}.
 *
 * @author Max Schwaab
 */
@DisplayName("UnleashProperties")
class UnleashPropertiesTest {

  private static final String MUST_NOT_BE_BLANK_MESSAGE = "must not be blank";

  private static UnleashProperties properties = new UnleashProperties();

  private ValidatorFactory validatorFactory;
  private Validator validator;

  @BeforeEach
  void setUp() {
    properties.setAppName("TestApp")
        .setApiUrl("http://unleash.com")
        .setApiClientSecret("c13n753cr37");
    validatorFactory = Validation.buildDefaultValidatorFactory();
    validator = validatorFactory.getValidator();
  }

  @AfterEach
  void tearDown() {
    validatorFactory.close();
  }

  @ParameterizedTest(name = "{0} violates validation on argument {2}")
  @MethodSource("validatedSettersWithInvalidArgs")
  void setValidatedStringPropertyShouldReturnViolationOnBlankString(final String methodName, final Function<String, UnleashProperties> setter, final String value) {
    assertThat(validator.validate(setter.apply(value)))
        .extracting(ConstraintViolation::getMessage)
        .containsExactly(MUST_NOT_BE_BLANK_MESSAGE);
  }

  private static Stream<Arguments> validatedSettersWithInvalidArgs() {
    final Function<String, UnleashProperties> setAppName = properties::setAppName;
    final Function<String, UnleashProperties> setApiUrl = properties::setApiUrl;
    final Function<String, UnleashProperties> setApiClientSecret = properties::setApiClientSecret;

    return Stream.of(
        Arguments.of("setAppName", setAppName, null),
        Arguments.of("setAppName", setAppName, ""),
        Arguments.of("setAppName", setAppName, " "),
        Arguments.of("setApiUrl", setApiUrl, null),
        Arguments.of("setApiUrl", setApiUrl, ""),
        Arguments.of("setApiUrl", setApiUrl, " "),
        Arguments.of("setApiClientSecret", setApiClientSecret, null),
        Arguments.of("setApiClientSecret", setApiClientSecret, ""),
        Arguments.of("setApiClientSecret", setApiClientSecret, " ")
    );
  }

}