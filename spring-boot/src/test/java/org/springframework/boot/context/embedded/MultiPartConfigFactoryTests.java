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

package org.springframework.boot.context.embedded;

import javax.servlet.MultipartConfigElement;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests for {@link MultiPartConfigFactory}.
 * 
 * @author Phillip Webb
 */
public class MultiPartConfigFactoryTests {

	@Test
	public void sensibleDefaults() {
		MultiPartConfigFactory factory = new MultiPartConfigFactory();
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getLocation(), equalTo(""));
		assertThat(config.getMaxFileSize(), equalTo(-1L));
		assertThat(config.getMaxRequestSize(), equalTo(-1L));
		assertThat(config.getFileSizeThreshold(), equalTo(0));
	}

	@Test
	public void create() throws Exception {
		MultiPartConfigFactory factory = new MultiPartConfigFactory();
		factory.setLocation("loc");
		factory.setMaxFileSize(1);
		factory.setMaxRequestSize(2);
		factory.setFileSizeThreshold(3);
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getLocation(), equalTo("loc"));
		assertThat(config.getMaxFileSize(), equalTo(1L));
		assertThat(config.getMaxRequestSize(), equalTo(2L));
		assertThat(config.getFileSizeThreshold(), equalTo(3));
	}

	@Test
	public void createWithStringSizes() throws Exception {
		MultiPartConfigFactory factory = new MultiPartConfigFactory();
		factory.setMaxFileSize("1");
		factory.setMaxRequestSize("2kB");
		factory.setFileSizeThreshold("3Mb");
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getMaxFileSize(), equalTo(1L));
		assertThat(config.getMaxRequestSize(), equalTo(2 * 1024L));
		assertThat(config.getFileSizeThreshold(), equalTo(3 * 1024 * 1024));
	}

}
