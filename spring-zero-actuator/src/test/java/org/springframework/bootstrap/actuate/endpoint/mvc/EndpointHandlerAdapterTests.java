/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.bootstrap.actuate.endpoint.mvc;

import org.junit.Test;
import org.springframework.bootstrap.actuate.endpoint.Endpoint;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link EndpointHandlerAdapter}.
 * 
 * @author Phillip Webb
 */
public class EndpointHandlerAdapterTests {

	private EndpointHandlerAdapter adapter = new EndpointHandlerAdapter();

	@Test
	public void onlySupportsEndpoints() throws Exception {
		assertTrue(this.adapter.supports(mock(Endpoint.class)));
		assertFalse(this.adapter.supports(mock(Object.class)));
	}

	// FIXME tests

}
