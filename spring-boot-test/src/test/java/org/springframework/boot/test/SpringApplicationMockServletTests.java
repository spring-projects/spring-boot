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

package org.springframework.boot.test;

import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationMockServletTests.Config;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.context.support.WebApplicationContextUtils;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link WebAppConfiguration} using {@link SpringApplicationContextLoader} with
 * a plain mock Servlet environment.
 *
 *
 * @author Andy Wilkinson
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = Config.class, loader = SpringApplicationContextLoader.class)
@WebAppConfiguration
@Deprecated
public class SpringApplicationMockServletTests {

	@Autowired
	private WebApplicationContext context;

	@Autowired
	private ServletContext servletContext;

	@Test
	public void webApplicationContextIsSetOnServletContext() {
		assertThat(this.context).isSameAs(
				WebApplicationContextUtils.getWebApplicationContext(this.servletContext));
	}

	@Configuration
	protected static class Config {

	}

}
