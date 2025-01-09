/*
 * Copyright 2012-2025 the original author or authors.
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

package org.springframework.boot.test.web.client;

import java.util.function.Supplier;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClient.Builder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MockServerRestClientCustomizer}.
 *
 * @author Scott Frederick
 */
class MockServerRestClientCustomizerTests {

	private MockServerRestClientCustomizer customizer;

	@BeforeEach
	void setup() {
		this.customizer = new MockServerRestClientCustomizer();
	}

	@Test
	void createShouldUseSimpleRequestExpectationManager() {
		MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer();
		customizer.customize(RestClient.builder());
		assertThat(customizer.getServer()).extracting("expectationManager")
			.isInstanceOf(SimpleRequestExpectationManager.class);
	}

	@Test
	void createWhenExpectationManagerClassIsNullShouldThrowException() {
		Class<? extends RequestExpectationManager> expectationManager = null;
		assertThatIllegalArgumentException().isThrownBy(() -> new MockServerRestClientCustomizer(expectationManager))
			.withMessageContaining("'expectationManager' must not be null");
	}

	@Test
	void createWhenExpectationManagerSupplierIsNullShouldThrowException() {
		Supplier<? extends RequestExpectationManager> expectationManagerSupplier = null;
		assertThatIllegalArgumentException()
			.isThrownBy(() -> new MockServerRestClientCustomizer(expectationManagerSupplier))
			.withMessageContaining("'expectationManagerSupplier' must not be null");
	}

	@Test
	void createShouldUseExpectationManagerClass() {
		MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer(
				UnorderedRequestExpectationManager.class);
		customizer.customize(RestClient.builder());
		assertThat(customizer.getServer()).extracting("expectationManager")
			.isInstanceOf(UnorderedRequestExpectationManager.class);
	}

	@Test
	void createShouldUseSupplier() {
		MockServerRestClientCustomizer customizer = new MockServerRestClientCustomizer(
				UnorderedRequestExpectationManager::new);
		customizer.customize(RestClient.builder());
		assertThat(customizer.getServer()).extracting("expectationManager")
			.isInstanceOf(UnorderedRequestExpectationManager.class);
	}

	@Test
	void customizeShouldBindServer() {
		Builder builder = RestClient.builder();
		this.customizer.customize(builder);
		this.customizer.getServer().expect(requestTo("/test")).andRespond(withSuccess());
		builder.build().get().uri("/test").retrieve().toEntity(String.class);
		this.customizer.getServer().verify();
	}

	@Test
	void getServerWhenNoServersAreBoundShouldThrowException() {
		assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
			.withMessageContaining("Unable to return a single MockRestServiceServer since "
					+ "MockServerRestClientCustomizer has not been bound to a RestClient");
	}

	@Test
	void getServerWhenMultipleServersAreBoundShouldThrowException() {
		this.customizer.customize(RestClient.builder());
		this.customizer.customize(RestClient.builder());
		assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
			.withMessageContaining("Unable to return a single MockRestServiceServer since "
					+ "MockServerRestClientCustomizer has been bound to more than one RestClient");
	}

	@Test
	void getServerWhenSingleServerIsBoundShouldReturnServer() {
		Builder builder = RestClient.builder();
		this.customizer.customize(builder);
		assertThat(this.customizer.getServer()).isEqualTo(this.customizer.getServer(builder));
	}

	@Test
	void getServerWhenRestClientBuilderIsFoundShouldReturnServer() {
		Builder builder1 = RestClient.builder();
		Builder builder2 = RestClient.builder();
		this.customizer.customize(builder1);
		this.customizer.customize(builder2);
		assertThat(this.customizer.getServer(builder1)).isNotNull();
		assertThat(this.customizer.getServer(builder2)).isNotNull().isNotSameAs(this.customizer.getServer(builder1));
	}

	@Test
	void getServerWhenRestClientBuilderIsNotFoundShouldReturnNull() {
		Builder builder1 = RestClient.builder();
		Builder builder2 = RestClient.builder();
		this.customizer.customize(builder1);
		assertThat(this.customizer.getServer(builder1)).isNotNull();
		assertThat(this.customizer.getServer(builder2)).isNull();
	}

	@Test
	void getServersShouldReturnServers() {
		Builder builder1 = RestClient.builder();
		Builder builder2 = RestClient.builder();
		this.customizer.customize(builder1);
		this.customizer.customize(builder2);
		assertThat(this.customizer.getServers()).containsOnlyKeys(builder1, builder2);
	}

	@Test
	void getExpectationManagersShouldReturnExpectationManagers() {
		Builder builder1 = RestClient.builder();
		Builder builder2 = RestClient.builder();
		this.customizer.customize(builder1);
		this.customizer.customize(builder2);
		RequestExpectationManager manager1 = this.customizer.getExpectationManagers().get(builder1);
		RequestExpectationManager manager2 = this.customizer.getExpectationManagers().get(builder2);
		assertThat(this.customizer.getServer(builder1)).extracting("expectationManager").isEqualTo(manager1);
		assertThat(this.customizer.getServer(builder2)).extracting("expectationManager").isEqualTo(manager2);
	}

}
