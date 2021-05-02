/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.context.properties;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import org.junit.jupiter.api.Test;

import org.springframework.boot.convert.ApplicationConversionService;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.ConversionService;
import org.springframework.core.convert.converter.Converter;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ConversionServiceDeducer}.
 *
 * @author Phillip Webb
 */
class ConversionServiceDeducerTests {

	@Test
	void getConversionServicesWhenHasConversionServiceBeanContainsOnlyBean() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				CustomConverterServiceConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		TestApplicationConversionService expected = applicationContext.getBean(TestApplicationConversionService.class);
		assertThat(deducer.getConversionServices()).containsExactly(expected);
	}

	@Test
	void getConversionServiceWhenHasNoConversionServiceBeanAndNoQualifiedBeansAndNoBeanFactoryConversionServiceReturnsEmptyList() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		assertThat(deducer.getConversionServices()).isEmpty();
	}

	@Test
	void getConversionServiceWhenHasNoConversionServiceBeanAndNoQualifiedBeansAndBeanFactoryConversionServiceContainsOnlyBeanFactoryInstance() {
		ConfigurableApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				EmptyConfiguration.class);
		ConversionService conversionService = new ApplicationConversionService();
		applicationContext.getBeanFactory().setConversionService(conversionService);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		List<ConversionService> conversionServices = deducer.getConversionServices();
		assertThat(conversionServices).containsOnly(conversionService);
		assertThat(conversionServices.get(0)).isSameAs(conversionService);
	}

	@Test
	void getConversionServiceWhenHasQualifiedConverterBeansContainsCustomizedApplicationService() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				CustomConverterConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		List<ConversionService> conversionServices = deducer.getConversionServices();
		assertThat(conversionServices).hasSize(1);
		assertThat(conversionServices.get(0)).isNotSameAs(ApplicationConversionService.getSharedInstance());
		assertThat(conversionServices.get(0).canConvert(InputStream.class, OutputStream.class)).isTrue();
	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConverterServiceConfiguration {

		@Bean(ConfigurableApplicationContext.CONVERSION_SERVICE_BEAN_NAME)
		TestApplicationConversionService conversionService() {
			return new TestApplicationConversionService();
		}

	}

	@Configuration(proxyBeanMethods = false)
	static class EmptyConfiguration {

	}

	@Configuration(proxyBeanMethods = false)
	static class CustomConverterConfiguration {

		@Bean
		@ConfigurationPropertiesBinding
		TestConverter testConverter() {
			return new TestConverter();
		}

	}

	private static class TestApplicationConversionService extends ApplicationConversionService {

	}

	private static class TestConverter implements Converter<InputStream, OutputStream> {

		@Override
		public OutputStream convert(InputStream source) {
			throw new UnsupportedOperationException();
		}

	}

}
