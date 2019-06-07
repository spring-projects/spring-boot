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

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.GenericServlet;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

/**
 * Tests for {@link EndpointServlet}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
public class EndpointServletTests {

	@Rule
	public ExpectedException thrown = ExpectedException.none();

	@Test
	public void createWhenServletClassIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Servlet must not be null");
		new EndpointServlet((Class<Servlet>) null);
	}

	@Test
	public void createWhenServletIsNullShouldThrowException() {
		this.thrown.expect(IllegalArgumentException.class);
		this.thrown.expectMessage("Servlet must not be null");
		new EndpointServlet((Servlet) null);
	}

	@Test
	public void createWithServletClassShouldCreateServletInstance() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		assertThat(endpointServlet.getServlet()).isInstanceOf(TestServlet.class);
	}

	@Test
	public void getServletShouldGetServlet() {
		TestServlet servlet = new TestServlet();
		EndpointServlet endpointServlet = new EndpointServlet(servlet);
		assertThat(endpointServlet.getServlet()).isEqualTo(servlet);
	}

	@Test
	public void withInitParameterNullName() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		this.thrown.expect(IllegalArgumentException.class);
		endpointServlet.withInitParameter(null, "value");
	}

	@Test
	public void withInitParameterEmptyName() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		this.thrown.expect(IllegalArgumentException.class);
		endpointServlet.withInitParameter(" ", "value");
	}

	@Test
	public void withInitParameterShouldReturnNewInstance() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		assertThat(endpointServlet.withInitParameter("spring", "boot")).isNotSameAs(endpointServlet);
	}

	@Test
	public void withInitParameterWhenHasExistingShouldMergeParameters() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class).withInitParameter("a", "b")
				.withInitParameter("c", "d");
		assertThat(endpointServlet.withInitParameter("a", "b1").withInitParameter("e", "f").getInitParameters())
				.containsExactly(entry("a", "b1"), entry("c", "d"), entry("e", "f"));
	}

	@Test
	public void withInitParametersNullName() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		this.thrown.expect(IllegalArgumentException.class);
		endpointServlet.withInitParameters(Collections.singletonMap(null, "value"));
	}

	@Test
	public void withInitParametersEmptyName() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		this.thrown.expect(IllegalArgumentException.class);
		endpointServlet.withInitParameters(Collections.singletonMap(" ", "value"));
	}

	@Test
	public void withInitParametersShouldCreateNewInstance() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class);
		assertThat(endpointServlet.withInitParameters(Collections.singletonMap("spring", "boot")))
				.isNotSameAs(endpointServlet);
	}

	@Test
	public void withInitParametersWhenHasExistingShouldMergeParameters() {
		EndpointServlet endpointServlet = new EndpointServlet(TestServlet.class).withInitParameter("a", "b")
				.withInitParameter("c", "d");
		Map<String, String> extra = new LinkedHashMap<>();
		extra.put("a", "b1");
		extra.put("e", "f");
		assertThat(endpointServlet.withInitParameters(extra).getInitParameters()).containsExactly(entry("a", "b1"),
				entry("c", "d"), entry("e", "f"));

	}

	private static class TestServlet extends GenericServlet {

		@Override
		public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException {
		}

	}

}
