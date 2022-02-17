/*
 * Copyright 2012-2022 the original author or authors.
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

package org.springframework.boot.web.servlet;

import jakarta.servlet.MultipartConfigElement;
import org.junit.jupiter.api.Test;

import org.springframework.util.unit.DataSize;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link MultipartConfigFactory}.
 *
 * @author Phillip Webb
 * @author Stephane Nicoll
 */
class MultipartConfigFactoryTests {

	@Test
	void sensibleDefaults() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getLocation()).isEqualTo("");
		assertThat(config.getMaxFileSize()).isEqualTo(-1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(-1L);
		assertThat(config.getFileSizeThreshold()).isEqualTo(0);
	}

	@Test
	void createWithDataSizes() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(1));
		factory.setMaxRequestSize(DataSize.ofKilobytes(2));
		factory.setFileSizeThreshold(DataSize.ofMegabytes(3));
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getMaxFileSize()).isEqualTo(1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(2 * 1024L);
		assertThat(config.getFileSizeThreshold()).isEqualTo(3 * 1024 * 1024);
	}

	@Test
	void createWithNegativeDataSizes() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(-1));
		factory.setMaxRequestSize(DataSize.ofKilobytes(-2));
		factory.setFileSizeThreshold(DataSize.ofMegabytes(-3));
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getMaxFileSize()).isEqualTo(-1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(-1);
		assertThat(config.getFileSizeThreshold()).isEqualTo(0);
	}

}
