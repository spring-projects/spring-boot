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

package org.springframework.boot.web.servlet.view;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.web.MockServletContext;
import org.springframework.web.servlet.View;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MustacheViewResolver}.
 *
 * @author Dave Syer
 * @author Andy Wilkinson
 */
class MustacheViewResolverTests {

	private final String prefix = "classpath:/" + getClass().getPackage().getName().replace(".", "/") + "/";

	private MustacheViewResolver resolver = new MustacheViewResolver();

	@BeforeEach
	void init() {
		GenericApplicationContext applicationContext = new GenericApplicationContext();
		applicationContext.refresh();
		this.resolver.setApplicationContext(applicationContext);
		this.resolver.setServletContext(new MockServletContext());
		this.resolver.setPrefix(this.prefix);
		this.resolver.setSuffix(".html");
	}

	@Test
	void resolveNonExistent() throws Exception {
		assertThat(this.resolver.resolveViewName("bar", null)).isNull();
	}

	@Test
	void resolveExisting() throws Exception {
		assertThat(this.resolver.resolveViewName("template", null)).isNotNull();
	}

	@Test
	void setsContentType() throws Exception {
		this.resolver.setContentType("application/octet-stream");
		View view = this.resolver.resolveViewName("template", null);
		assertThat(view.getContentType()).isEqualTo("application/octet-stream");

	}

}
