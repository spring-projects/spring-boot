/*
 * Copyright 2012-2020 the original author or authors.
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
	void getConversionServiceWhenHasConversionServiceBeanReturnsBean() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				CustomConverterServiceConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		assertThat(deducer.getConversionService()).isInstanceOf(TestApplicationConversionService.class);
	}

	@Test
	void getConversionServiceWhenHasNoConversionServiceBeanAndNoQualifiedBeansReturnsSharedInstance() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(EmptyConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		assertThat(deducer.getConversionService()).isSameAs(ApplicationConversionService.getSharedInstance());
	}

	@Test
	void getConversionServiceWhenHasQualifiedConverterBeansReturnsNewInstance() {
		ApplicationContext applicationContext = new AnnotationConfigApplicationContext(
				CustomConverterConfiguration.class);
		ConversionServiceDeducer deducer = new ConversionServiceDeducer(applicationContext);
		ConversionService conversionService = deducer.getConversionService();
		assertThat(conversionService).isNotSameAs(ApplicationConversionService.getSharedInstance());
		assertThat(conversionService.canConvert(InputStream.class, OutputStream.class)).isTrue();
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
