/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.actuate.metrics.web.reactive.client;

import java.io.IOException;
import java.net.URI;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link WebClientExchangeTags}.
 *
 * @author Brian Clozel
 * @author Nishant Raut
 */
class WebClientExchangeTagsTests {

	private static final String URI_TEMPLATE_ATTRIBUTE = WebClient.class.getName() + ".uriTemplate";

	private ClientRequest request;

	private ClientResponse response;

	@BeforeEach
	void setup() {
		this.request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "https://example.org/projects/{project}").build();
		this.response = mock(ClientResponse.class);
	}

	@Test
	void method() {
		assertThat(WebClientExchangeTags.method(this.request)).isEqualTo(Tag.of("method", "GET"));
	}

	@Test
	void uriWhenAbsoluteTemplateIsAvailableShouldReturnTemplate() {
		assertThat(WebClientExchangeTags.uri(this.request)).isEqualTo(Tag.of("uri", "/projects/{project}"));
	}

	@Test
	void uriWhenRelativeTemplateIsAvailableShouldReturnTemplate() {
		this.request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot"))
				.attribute(URI_TEMPLATE_ATTRIBUTE, "/projects/{project}").build();
		assertThat(WebClientExchangeTags.uri(this.request)).isEqualTo(Tag.of("uri", "/projects/{project}"));
	}

	@Test
	void uriWhenTemplateIsMissingShouldReturnPath() {
		this.request = ClientRequest.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot"))
				.build();
		assertThat(WebClientExchangeTags.uri(this.request)).isEqualTo(Tag.of("uri", "/projects/spring-boot"));
	}

	@Test
	void uriWhenTemplateIsMissingShouldReturnPathWithQueryParams() {
		this.request = ClientRequest
				.create(HttpMethod.GET, URI.create("https://example.org/projects/spring-boot?section=docs")).build();
		assertThat(WebClientExchangeTags.uri(this.request))
				.isEqualTo(Tag.of("uri", "/projects/spring-boot?section=docs"));
	}

	@Test
	void clientName() {
		assertThat(WebClientExchangeTags.clientName(this.request)).isEqualTo(Tag.of("client.name", "example.org"));
	}

	@Test
	void status() {
		given(this.response.statusCode()).willReturn(HttpStatus.OK);
		assertThat(WebClientExchangeTags.status(this.response, null)).isEqualTo(Tag.of("status", "200"));
	}

	@Test
	void statusWhenIOException() {
		assertThat(WebClientExchangeTags.status(null, new IOException())).isEqualTo(Tag.of("status", "IO_ERROR"));
	}

	@Test
	void statusWhenClientException() {
		assertThat(WebClientExchangeTags.status(null, new IllegalArgumentException()))
				.isEqualTo(Tag.of("status", "CLIENT_ERROR"));
	}

	@Test
	void statusWhenNonStandard() {
		given(this.response.statusCode()).willReturn(HttpStatusCode.valueOf(490));
		assertThat(WebClientExchangeTags.status(this.response, null)).isEqualTo(Tag.of("status", "490"));
	}

	@Test
	void statusWhenCancelled() {
		assertThat(WebClientExchangeTags.status(null, null)).isEqualTo(Tag.of("status", "CLIENT_ERROR"));
	}

	@Test
	void outcomeTagIsUnknownWhenResponseIsNull() {
		Tag tag = WebClientExchangeTags.outcome(null);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

	@Test
	void outcomeTagIsInformationalWhenResponseIs1xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.CONTINUE);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("INFORMATIONAL");
	}

	@Test
	void outcomeTagIsSuccessWhenResponseIs2xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.OK);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("SUCCESS");
	}

	@Test
	void outcomeTagIsRedirectionWhenResponseIs3xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.MOVED_PERMANENTLY);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("REDIRECTION");
	}

	@Test
	void outcomeTagIsClientErrorWhenResponseIs4xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.BAD_REQUEST);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("CLIENT_ERROR");
	}

	@Test
	void outcomeTagIsServerErrorWhenResponseIs5xx() {
		given(this.response.statusCode()).willReturn(HttpStatus.BAD_GATEWAY);
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("SERVER_ERROR");
	}

	@Test
	void outcomeTagIsClientErrorWhenResponseIsNonStandardInClientSeries() {
		given(this.response.statusCode()).willReturn(HttpStatusCode.valueOf(490));
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("CLIENT_ERROR");
	}

	@Test
	void outcomeTagIsUnknownWhenResponseStatusIsInUnknownSeries() {
		given(this.response.statusCode()).willReturn(HttpStatusCode.valueOf(701));
		Tag tag = WebClientExchangeTags.outcome(this.response);
		assertThat(tag.getValue()).isEqualTo("UNKNOWN");
	}

}
