/*
 * Copyright 2012-2016 the original author or authors.
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

package org.springframework.boot.web.servlet;

import javax.servlet.MultipartConfigElement;

import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartConfigFactory}.
 *
 * @author Phillip Webb
 */
public class MultipartConfigFactoryTests {

	@Test
	public void sensibleDefaults() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getLocation()).isEqualTo("");
		assertThat(config.getMaxFileSize()).isEqualTo(-1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(-1L);
		assertThat(config.getFileSizeThreshold()).isEqualTo(0);
	}

	@Test
	public void create() throws Exception {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setLocation("loc");
		factory.setMaxFileSize(1);
		factory.setMaxRequestSize(2);
		factory.setFileSizeThreshold(3);
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getLocation()).isEqualTo("loc");
		assertThat(config.getMaxFileSize()).isEqualTo(1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(2L);
		assertThat(config.getFileSizeThreshold()).isEqualTo(3);
	}

	@Test
	public void createWithStringSizes() throws Exception {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize("1");
		factory.setMaxRequestSize("2kB");
		factory.setFileSizeThreshold("3Mb");
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getMaxFileSize()).isEqualTo(1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(2 * 1024L);
		assertThat(config.getFileSizeThreshold()).isEqualTo(3 * 1024 * 1024);
	}

}
