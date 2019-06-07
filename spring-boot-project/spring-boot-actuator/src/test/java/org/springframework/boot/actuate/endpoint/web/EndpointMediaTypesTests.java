/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.endpoint.web;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link EndpointMediaTypes}.
 *
 * @author Phillip Webb
 */
public class EndpointMediaTypesTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenProducedIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Produced must not be null");
		new EndpointMediaTypes(null, Collections.emptyList());
	}

	@Test
	public void createWhenConsumedIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Consumed must not be null");
		new EndpointMediaTypes(Collections.emptyList(), null);
	}

	@Test
	public void getProducedShouldReturnProduced() {
		List<String> produced = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(produced, Collections.emptyList());
		assertThat(types.getProduced()).isEqualTo(produced);
	}

	@Test
	public void getConsumedShouldReturnConsumed() {
		List<String> consumed = Arrays.asList("a", "b", "c");
		EndpointMediaTypes types = new EndpointMediaTypes(Collections.emptyList(), consumed);
		assertThat(types.getConsumed()).isEqualTo(consumed);
	}

}
