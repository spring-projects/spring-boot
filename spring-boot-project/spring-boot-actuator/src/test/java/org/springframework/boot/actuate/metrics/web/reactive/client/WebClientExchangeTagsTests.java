/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import java.io.IOException;
import java.net.URI;

import io.micrometer.core.instrument.Tag;
import org.junit.Before;
import org.junit.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientExchangeTags}
 *
 * @author Brian Clozel
 * @author Nishant Raut
 */
public class WebClientExchangeTagsTests {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName()
			+ ".uriTemplate";

	private ClientRequest request;

	private ClientResponse response;

	@Before
	public void setup() {
		this.request = ClientRequest
				.create(HttpMethod.GET,
						URI.create("http://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE,
						"http://example.org/projects/{project}")
				.build();
		this.response = mock(ClientResponse.class);
		given(this.response.statusCode()).willReturn(HttpStatus.OK);
	}

	@Test
	public void method() {
		assertThat(WebClientExchangeTags.method(this.request))
				.isEqualTo(Tag.of("method", "GET"));
	}

	@Test
	public void uriWhenAbsoluteTemplateIsAvailableShouldReturnTemplate() {
		assertThat(WebClientExchangeTags.uri(this.request))
				.isEqualTo(Tag.of("uri", "/projects/{project}"));
	}

	@Test
	public void uriWhenRelativeTemplateIsAvailableShouldReturnTemplate() {
		this.request = ClientRequest
				.create(HttpMethod.GET,
						URI.create("http://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "/projects/{project}").build();
		assertThat(WebClientExchangeTags.uri(this.request))
				.isEqualTo(Tag.of("uri", "/projects/{project}"));
	}

	@Test
	public void uriWhenTemplateIsMissingShouldReturnPath() {
		this.request = ClientRequest.create(HttpMethod.GET,
				URI.create("http://example.org/projects/spring-boot")).build();
		assertThat(WebClientExchangeTags.uri(this.request))
				.isEqualTo(Tag.of("uri", "/projects/spring-boot"));
	}

	@Test
	public void clientName() {
		assertThat(WebClientExchangeTags.clientName(this.request))
				.isEqualTo(Tag.of("clientName", "example.org"));
	}

	@Test
	public void status() {
		assertThat(WebClientExchangeTags.status(this.response))
				.isEqualTo(Tag.of("status", "200"));
	}

	@Test
	public void statusWhenIOException() {
		assertThat(WebClientExchangeTags.status(new IOException()))
				.isEqualTo(Tag.of("status", "IO_ERROR"));
	}

	@Test
	public void statusWhenClientException() {
		assertThat(WebClientExchangeTags.status(new IllegalArgumentException()))
				.isEqualTo(Tag.of("status", "CLIENT_ERROR"));
	}

	@Test
	public void outcomeTagIsUnknownWhenResponseStatusIsNull() {
		Tag tag = WebClientExchangeTags.outcome(null);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	public void outcomeTagIsInformationalWhenResponseIs1xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.CONTINUE);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("INFORMATIONAL");
	}

	@Test
	public void outcomeTagIsSuccessWhenResponseIs2xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.OK);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("SUCCESS");
	}

	@Test
	public void outcomeTagIsRedirectionWhenResponseIs3xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	public void outcomeTagIsClientErrorWhenResponseIs4xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.BAD_REQUEST);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("CLIENT_ERROR");
	}

	@Test
	public void outcomeTagIsServerErrorWhenResponseIs5xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.BAD_GATEWAY);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("SERVER_ERROR");
	}

	@Test
	public void outcomeTagIsUknownWhenResponseStatusIsUknown() {
		given(this.response.statusCode()).willThrow(IllegalArgumentException.class);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

}
