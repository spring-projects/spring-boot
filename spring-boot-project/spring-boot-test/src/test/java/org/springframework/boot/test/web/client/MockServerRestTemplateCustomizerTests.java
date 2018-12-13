/*
 * Copyright 2012-2018 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.test.web.client;

import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.test.web.client.RequestExpectationManager;
import org.springframework.test.web.client.SimpleRequestExpectationManager;
import org.springframework.test.web.client.UnorderedRequestExpectationManager;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

/**
 * Tests for {@link MockServerRestTemplateCustomizer}.
 *
 * @author Phillip Webb
 */
public class MockServerRestTemplateCustomizerTests {

	private MockServerRestTemplateCustomizer customizer;

	@Before
	public void setup() {
		this.customizer = new MockServerRestTemplateCustomizer();
	}

	@Test
	public void createShouldUseSimpleRequestExpectationManager() {
		MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer();
		customizer.customize(new RestTemplate());
		assertThat(customizer.getServer()).extracting("expectationManager")
				.hasAtLeastOneElementOfType(SimpleRequestExpectationManager.class);
	}

	@Test
	public void createWhenExpectationManagerClassIsNullShouldThrowException() {
		assertThatIllegalArgumentException()
				.isThrownBy(() -> new MockServerRestTemplateCustomizer(null))
				.withMessageContaining("ExpectationManager must not be null");
	}

	@Test
	public void createShouldUseExpectationManagerClass() {
		MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer(
				UnorderedRequestExpectationManager.class);
		customizer.customize(new RestTemplate());
		assertThat(customizer.getServer()).extracting("expectationManager")
				.hasAtLeastOneElementOfType(UnorderedRequestExpectationManager.class);
	}

	@Test
	public void detectRootUriShouldDefaultToTrue() {
		MockServerRestTemplateCustomizer customizer = new MockServerRestTemplateCustomizer(
				UnorderedRequestExpectationManager.class);
		customizer.customize(
				new RestTemplateBuilder().rootUri("http://example.com").build());
		assertThat(customizer.getServer()).extracting("expectationManager")
				.hasAtLeastOneElementOfType(RootUriRequestExpectationManager.class);
	}

	@Test
	public void setDetectRootUriShouldDisableRootUriDetection() {
		this.customizer.setDetectRootUri(false);
		this.customizer.customize(
				new RestTemplateBuilder().rootUri("http://example.com").build());
		assertThat(this.customizer.getServer()).extracting("expectationManager")
				.hasAtLeastOneElementOfType(SimpleRequestExpectationManager.class);

	}

	@Test
	public void customizeShouldBindServer() {
		RestTemplate template = new RestTemplateBuilder(this.customizer).build();
		this.customizer.getServer().expect(requestTo("/test")).andRespond(withSuccess());
		template.getForEntity("/test", String.class);
		this.customizer.getServer().verify();
	}

	@Test
	public void getServerWhenNoServersAreBoundShouldThrowException() {
		assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
				.withMessageContaining(
						"Unable to return a single MockRestServiceServer since "
								+ "MockServerRestTemplateCustomizer has not been bound to a RestTemplate");
	}

	@Test
	public void getServerWhenMultipleServersAreBoundShouldThrowException() {
		this.customizer.customize(new RestTemplate());
		this.customizer.customize(new RestTemplate());
		assertThatIllegalStateException().isThrownBy(this.customizer::getServer)
				.withMessageContaining(
						"Unable to return a single MockRestServiceServer since "
								+ "MockServerRestTemplateCustomizer has been bound to more than one RestTemplate");
	}

	@Test
	public void getServerWhenSingleServerIsBoundShouldReturnServer() {
		RestTemplate template = new RestTemplate();
		this.customizer.customize(template);
		assertThat(this.customizer.getServer())
				.isEqualTo(this.customizer.getServer(template));
	}

	@Test
	public void getServerWhenRestTemplateIsFoundShouldReturnServer() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.customizer.customize(template1);
		this.customizer.customize(template2);
		assertThat(this.customizer.getServer(template1)).isNotNull();
		assertThat(this.customizer.getServer(template2)).isNotNull()
				.isNotSameAs(this.customizer.getServer(template1));
	}

	@Test
	public void getServerWhenRestTemplateIsNotFoundShouldReturnNull() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.customizer.customize(template1);
		assertThat(this.customizer.getServer(template1)).isNotNull();
		assertThat(this.customizer.getServer(template2)).isNull();
	}

	@Test
	public void getServersShouldReturnServers() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.customizer.customize(template1);
		this.customizer.customize(template2);
		assertThat(this.customizer.getServers()).containsOnlyKeys(template1, template2);
	}

	@Test
	public void getExpectationManagersShouldReturnExpectationManagers() {
		RestTemplate template1 = new RestTemplate();
		RestTemplate template2 = new RestTemplate();
		this.customizer.customize(template1);
		this.customizer.customize(template2);
		RequestExpectationManager manager1 = this.customizer.getExpectationManagers()
				.get(template1);
		RequestExpectationManager manager2 = this.customizer.getExpectationManagers()
				.get(template2);
		assertThat(this.customizer.getServer(template1)).extracting("expectationManager")
				.containsOnly(manager1);
		assertThat(this.customizer.getServer(template2)).extracting("expectationManager")
				.containsOnly(manager2);
	}

}
