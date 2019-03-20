/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.Condition;
import org.junit.Before;
import org.junit.Test;

import org.springframework.boot.actuate.endpoint.AbstractEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.AbstractMvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.EndpointMvcAdapter;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoint;
import org.springframework.boot.actuate.endpoint.mvc.MvcEndpoints;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.ResourceSupport;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.context.support.StaticWebApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link LinksEnhancer}.
 *
 * @author Madhura Bhave
 */
public class LinksEnhancerTests {

	@Before
	public void setup() {
		MockHttpServletRequest request = new MockHttpServletRequest();
		RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
	}

	@Test
	public void useNameAsRelIfAvailable() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		endpoint.setPath("something-else");
		LinksEnhancer enhancer = getLinksEnhancer(
				Collections.singletonList((MvcEndpoint) endpoint));
		ResourceSupport support = new ResourceSupport();
		enhancer.addEndpointLinks(support, "");
		assertThat(support.getLink("a").getHref()).contains("/something-else");
	}

	@Test
	public void usePathAsRelIfNameNotAvailable() throws Exception {
		MvcEndpoint endpoint = new NoNameTestMvcEndpoint("/a", false);
		LinksEnhancer enhancer = getLinksEnhancer(Collections.singletonList(endpoint));
		ResourceSupport support = new ResourceSupport();
		enhancer.addEndpointLinks(support, "");
		assertThat(support.getLink("a").getHref()).contains("/a");
	}

	@Test
	public void hrefNotAddedToRelTwice() throws Exception {
		MvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		MvcEndpoint otherEndpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		LinksEnhancer enhancer = getLinksEnhancer(Arrays.asList(endpoint, otherEndpoint));
		ResourceSupport support = new ResourceSupport();
		enhancer.addEndpointLinks(support, "");
		assertThat(support.getLinks()).haveExactly(1, getCondition("a", "a"));
	}

	@Test
	public void multipleHrefsForSameRelWhenPathIsDifferent() throws Exception {
		TestMvcEndpoint endpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		endpoint.setPath("endpoint");
		TestMvcEndpoint otherEndpoint = new TestMvcEndpoint(new TestEndpoint("a"));
		otherEndpoint.setPath("other-endpoint");
		LinksEnhancer enhancer = getLinksEnhancer(
				Arrays.asList((MvcEndpoint) endpoint, otherEndpoint));
		ResourceSupport support = new ResourceSupport();
		enhancer.addEndpointLinks(support, "");
		assertThat(support.getLinks()).haveExactly(1, getCondition("a", "endpoint"));
		assertThat(support.getLinks()).haveExactly(1,
				getCondition("a", "other-endpoint"));
	}

	private LinksEnhancer getLinksEnhancer(List<MvcEndpoint> endpoints) throws Exception {
		StaticWebApplicationContext context = new StaticWebApplicationContext();
		for (MvcEndpoint endpoint : endpoints) {
			context.getDefaultListableBeanFactory().registerSingleton(endpoint.toString(),
					endpoint);
		}
		MvcEndpoints mvcEndpoints = new MvcEndpoints();
		mvcEndpoints.setApplicationContext(context);
		mvcEndpoints.afterPropertiesSet();
		return new LinksEnhancer("", mvcEndpoints);
	}

	private Condition<Link> getCondition(final String rel, final String href) {
		return new Condition<Link>() {

			@Override
			public boolean matches(Link link) {
				return link.getRel().equals(rel)
						&& link.getHref().equals("http://localhost/" + href);
			}

		};
	}

	private static class TestEndpoint extends AbstractEndpoint<Object> {

		TestEndpoint(String id) {
			super(id);
		}

		@Override
		public Object invoke() {
			return null;
		}

	}

	private static class TestMvcEndpoint extends EndpointMvcAdapter {

		TestMvcEndpoint(TestEndpoint delegate) {
			super(delegate);
		}

	}

	private static class NoNameTestMvcEndpoint extends AbstractMvcEndpoint {

		NoNameTestMvcEndpoint(String path, boolean sensitive) {
			super(path, sensitive);
		}

	}

}
