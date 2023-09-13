/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.autoconfigure.web.client;

import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link RestClientBuilderConfigurer}.
 *
 * @author Moritz Halbritter
 */
class RestClientBuilderConfigurerTests {

	@Test
	void shouldApplyCustomizers() {
		RestClientBuilderConfigurer configurer = new RestClientBuilderConfigurer();
		RestClientCustomizer customizer = mock(RestClientCustomizer.class);
		configurer.setRestClientCustomizers(List.of(customizer));
		RestClient.Builder builder = RestClient.builder();
		configurer.configure(builder);
		then(customizer).should().customize(builder);
	}

	@Test
	void shouldSupportNullAsCustomizers() {
		RestClientBuilderConfigurer configurer = new RestClientBuilderConfigurer();
		configurer.setRestClientCustomizers(null);
		assertThatCode(() -> configurer.configure(RestClient.builder())).doesNotThrowAnyException();
	}

}
