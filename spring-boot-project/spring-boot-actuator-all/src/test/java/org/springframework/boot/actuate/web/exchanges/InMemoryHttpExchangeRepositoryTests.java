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

package org.springframework.boot.actuate.web.exchanges;

import java.security.Principal;
import java.util.List;
import java.util.function.Supplier;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link InMemoryHttpExchangeRepository}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class InMemoryHttpExchangeRepositoryTests {

	private static final Supplier<Principal> NO_PRINCIPAL = () -> null;

	private static final Supplier<String> NO_SESSION_ID = () -> null;

	private final InMemoryHttpExchangeRepository repository = new InMemoryHttpExchangeRepository();

	@Test
	void adWhenHasLimitedCapacityRestrictsSize() {
		this.repository.setCapacity(2);
		this.repository.add(createHttpExchange("GET"));
		this.repository.add(createHttpExchange("POST"));
		this.repository.add(createHttpExchange("DELETE"));
		List<HttpExchange> exchanges = this.repository.findAll();
		assertThat(exchanges).hasSize(2);
		assertThat(exchanges.get(0).getRequest().getMethod()).isEqualTo("DELETE");
		assertThat(exchanges.get(1).getRequest().getMethod()).isEqualTo("POST");
	}

	@Test
	void addWhenReverseFalseReturnsInCorrectOrder() {
		this.repository.setReverse(false);
		this.repository.setCapacity(2);
		this.repository.add(createHttpExchange("GET"));
		this.repository.add(createHttpExchange("POST"));
		this.repository.add(createHttpExchange("DELETE"));
		List<HttpExchange> exchanges = this.repository.findAll();
		assertThat(exchanges).hasSize(2);
		assertThat(exchanges.get(0).getRequest().getMethod()).isEqualTo("POST");
		assertThat(exchanges.get(1).getRequest().getMethod()).isEqualTo("DELETE");
	}

	private HttpExchange createHttpExchange(String method) {
		RecordableHttpRequest request = mock(RecordableHttpRequest.class);
		given(request.getMethod()).willReturn(method);
		RecordableHttpResponse response = mock(RecordableHttpResponse.class);
		return HttpExchange.start(request).finish(response, NO_PRINCIPAL, NO_SESSION_ID);
	}

}
