/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.servlet;

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
		assertThat(config.getLocation()).isEmpty();
		assertThat(config.getMaxFileSize()).isEqualTo(-1L);
		assertThat(config.getMaxRequestSize()).isEqualTo(-1L);
		assertThat(config.getFileSizeThreshold()).isZero();
	}

	@Test
	void createWithDataSizes() {
		MultipartConfigFactory factory = new MultipartConfigFactory();
		factory.setMaxFileSize(DataSize.ofBytes(1));
		factory.setMaxRequestSize(DataSize.ofKilobytes(2));
		factory.setFileSizeThreshold(DataSize.ofMegabytes(3));
		MultipartConfigElement config = factory.createMultipartConfig();
		assertThat(config.getMaxFileSize()).isOne();
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
		assertThat(config.getFileSizeThreshold()).isZero();
	}

}
