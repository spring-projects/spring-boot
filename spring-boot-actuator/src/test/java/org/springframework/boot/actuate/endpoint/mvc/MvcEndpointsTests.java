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

package org.springframework.boot.actuate.endpoint.mvc;

import org.junit.Test;
import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.context.support.StaticApplicationContext;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link MvcEndpoints}.
 *
 * @author Dave Syer
 */
public class MvcEndpointsTests {

	private MvcEndpoints endpoints = new MvcEndpoints();

	private StaticApplicationContext context = new StaticApplicationContext();

	@Test
	public void picksUpEndpointDelegates() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new TestEndpoint());
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertEquals(1, this.endpoints.getEndpoints().size());
	}

	@Test
	public void picksUpEndpointDelegatesFromParent() throws Exception {
		StaticApplicationContext parent = new StaticApplicationContext();
		this.context.setParent(parent);
		parent.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new TestEndpoint());
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertEquals(1, this.endpoints.getEndpoints().size());
	}

	@Test
	public void picksUpMvcEndpoints() throws Exception {
		this.context.getDefaultListableBeanFactory().registerSingleton("endpoint",
				new EndpointMvcAdapter(new TestEndpoint()));
		this.endpoints.setApplicationContext(this.context);
		this.endpoints.afterPropertiesSet();
		assertEquals(1, this.endpoints.getEndpoints().size());
	}

	protected static class TestEndpoint extends AbstractEndpoint<String> {

		public TestEndpoint() {
			super("test");
		}

		@Override
		public String invoke() {
			return "foo";
		}
	}

}
