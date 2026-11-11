/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.actuate.web.exchanges;

import java.security.Principal;
import java.util.List;
import java.util.function.Supplier;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link HttpExchangesEndpoint}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
class HttpExchangesEndpointTests {

	private static final Supplier<@Nullable Principal> NO_PRINCIPAL = () -> null;

	private static final Supplier<@Nullable String> NO_SESSION_ID = () -> null;

	@Test
	void httpExchanges() {
		HttpExchangeRepository repository = new InMemoryHttpExchangeRepository();
		repository.add(HttpExchange.start(createRequest("GET")).finish(createResponse(), NO_PRINCIPAL, NO_SESSION_ID));
		List<HttpExchange> httpExchanges = new HttpExchangesEndpoint(repository).httpExchanges().getExchanges();
		assertThat(httpExchanges).hasSize(1);
		HttpExchange exchange = httpExchanges.get(0);
		assertThat(exchange.getRequest().getMethod()).isEqualTo("GET");
	}

	private RecordableHttpRequest createRequest(String method) {
		RecordableHttpRequest request = mock(RecordableHttpRequest.class);
		given(request.getMethod()).willReturn(method);
		return request;
	}

	private RecordableHttpResponse createResponse() {
		return mock(RecordableHttpResponse.class);
	}

}
