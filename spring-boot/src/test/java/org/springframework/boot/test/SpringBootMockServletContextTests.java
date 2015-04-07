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

import java.io.File;
import java.io.FilenameFilter;
import java.net.MalformedURLException;
import java.net.URL;

import javax.servlet.ServletContext;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ServletContextAware;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link SpringBootMockServletContext}.
 *
 * @author Phillip Webb
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = SpringBootMockServletContextTests.Config.class)
@WebAppConfiguration("src/test/webapp")
public class SpringBootMockServletContextTests implements ServletContextAware {

	private ServletContext servletContext;

	@Override
	public void setServletContext(ServletContext servletContext) {
		this.servletContext = servletContext;
	}

	@Test
	public void getResourceLocation() throws Exception {
		testResource("/inwebapp", "src/test/webapp");
		testResource("/inmetainfresources", "/META-INF/resources");
		testResource("/inresources", "/resources");
		testResource("/instatic", "/static");
		testResource("/inpublic", "/public");
	}

	private void testResource(String path, String expectedLocation)
			throws MalformedURLException {
		URL resource = this.servletContext.getResource(path);
		assertThat("Path " + path + " not found", resource, not(nullValue()));
		assertThat("Path " + path + " not in " + expectedLocation, resource.getPath(),
				containsString(expectedLocation));
	}

	// gh-2654
	@Test
	public void getRootUrlExistsAndIsEmpty() throws Exception {
		SpringBootMockServletContext context = new SpringBootMockServletContext(
				"src/test/doesntexist") {
			@Override
			protected String getResourceLocation(String path) {
				// Don't include the Spring Boot defaults for this test
				return getResourceBasePathLocation(path);
			};
		};
		URL resource = context.getResource("/");
		assertThat(resource, not(nullValue()));
		File file = new File(resource.getPath());
		assertThat("File " + file + " exists", file.exists(), equalTo(true));
		assertThat("File " + file + " is directory", file.isDirectory(), equalTo(true));
		String[] contents = file.list(new FilenameFilter() {
			@Override
			public boolean accept(File dir, String name) {
				return !(".".equals(name) || "..".equals(name));
			}
		});
		assertThat(contents, not(nullValue()));
		assertThat(contents.length, equalTo(0));
	}

	@Configuration
	static class Config {

	}

}
