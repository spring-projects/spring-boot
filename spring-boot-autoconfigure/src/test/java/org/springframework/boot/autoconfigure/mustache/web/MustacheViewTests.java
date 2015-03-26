/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.mustache.web;

import java.util.Collections;

import org.junit.Before;
import org.junit.Test;
import org.springframework.boot.autoconfigure.mustache.web.MustacheView;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.AnnotationConfigWebApplicationContext;

import com.samskivert.mustache.Mustache;

import static org.junit.Assert.assertEquals;

/**
 * Tests for {@link MustacheView}.
 *
 * @author Dave Syer
 */
public class MustacheViewTests {

	private MockHttpServletRequest request = new MockHttpServletRequest();

	private MockHttpServletResponse response = new MockHttpServletResponse();

	private AnnotationConfigWebApplicationContext context = new AnnotationConfigWebApplicationContext();

	@Before
	public void init() {
		this.context.refresh();
		MockServletContext servletContext = new MockServletContext();
		this.context.setServletContext(servletContext);
		servletContext.setAttribute(
				WebApplicationContext.ROOT_WEB_APPLICATION_CONTEXT_ATTRIBUTE,
				this.context);
	}

	@Test
	public void viewResolvesHandlebars() throws Exception {
		MustacheView view = new MustacheView(Mustache.compiler().compile("Hello {{msg}}"));
		view.setApplicationContext(this.context);
		view.render(Collections.singletonMap("msg", "World"), this.request, this.response);
		assertEquals("Hello World", this.response.getContentAsString());
	}

}
