/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.test.mock.web;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.ServletContext;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import org.springframework.boot.test.context.SpringBootContextLoader;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ServletContextAware;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link SpringBootMockServletContext}.
 *
 * @author Phillip Webb
 */
@DirtiesContext
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = SpringBootContextLoader.class)
@WebAppConfiguration("src/test/webapp")
class SpringBootMockServletContextTests implements ServletContextAware {

	@SuppressWarnings("NullAway.Init")
	private ServletContext servletContext;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Test
	void getResourceLocation() throws Exception {
		testResource("/inwebapp", "src/test/webapp");
		testResource("/inmetainfresources", "/META-INF/resources");
		testResource("/inresources", "/resources");
		testResource("/instatic", "/static");
		testResource("/inpublic", "/public");
	}

	private void testResource(String path, String expectedLocation) throws MalformedURLException {
		URL resource = this.servletContext.getResource(path);
		assertThat(resource).isNotNull();
		assertThat(resource.getPath()).contains(expectedLocation);
	}

	// gh-2654
	@Test
	void getRootUrlExistsAndIsEmpty() throws Exception {
		SpringBootMockServletContext context = new SpringBootMockServletContext("src/test/doesntexist") {
			@Override
			protected String getResourceLocation(String path) {
				// Don't include the Spring Boot defaults for this test
				return getResourceBasePathLocation(path);
			}
		};
		URL resource = context.getResource("/");
		assertThat(resource).isNotNull();
		File file = new File(URLDecoder.decode(resource.getPath(), StandardCharsets.UTF_8));
		assertThat(file).exists().isDirectory();
		String[] contents = file.list((dir, name) -> !(".".equals(name) || "..".equals(name)));
		assertThat(contents).isNotNull();
		assertThat(contents).isEmpty();
	}

	@Configuration(proxyBeanMethods = false)
	static class Config {

	}

}
