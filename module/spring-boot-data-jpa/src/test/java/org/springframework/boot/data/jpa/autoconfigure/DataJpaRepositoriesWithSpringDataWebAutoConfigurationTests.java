/*
 * Copyright 2012-present the original author or authors.
 */

package org.springframework.boot.data.jpa.autoconfigure;

import org.junit.jupiter.api.Test;

import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.data.autoconfigure.web.DataWebAutoConfiguration;
import org.springframework.boot.data.jpa.autoconfigure.domain.city.City;
import org.springframework.boot.data.jpa.autoconfigure.domain.city.CityRepository;
import org.springframework.boot.hibernate.autoconfigure.HibernateJpaAutoConfiguration;
import org.springframework.boot.jdbc.autoconfigure.DataSourceAutoConfiguration;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.geo.Distance;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.SortHandlerMethodArgumentResolver;
import org.springframework.format.support.FormattingConversionService;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link DataWebAutoConfiguration} and
 * {@link DataJpaRepositoriesAutoConfiguration}.
 *
 * @author Dave Syer
 * @author Stephane Nicoll
 */
class DataJpaRepositoriesWithSpringDataWebAutoConfigurationTests {

	private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
		.withConfiguration(AutoConfigurations.of(DataSourceAutoConfiguration.class, HibernateJpaAutoConfiguration.class,
				DataJpaRepositoriesAutoConfiguration.class, DataWebAutoConfiguration.class))
		.withPropertyValues("spring.datasource.generate-unique-name=true");

	@Test
	void springDataWebIsConfiguredWithJpaRepositories() {
		this.contextRunner.withUserConfiguration(TestConfiguration.class).run((context) -> {
			assertThat(context).hasSingleBean(CityRepository.class);
			assertThat(context).hasSingleBean(PageableHandlerMethodArgumentResolver.class);
			assertThat(context).hasSingleBean(SortHandlerMethodArgumentResolver.class);
			assertThat(context.getBean(FormattingConversionService.class).canConvert(String.class, Distance.class))
				.isTrue();
		});
	}

	@Configuration(proxyBeanMethods = false)
	@TestAutoConfigurationPackage(City.class)
	@EnableWebMvc
	static class TestConfiguration {

	}

}
