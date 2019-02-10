/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.autoconfigure.cache;

import org.junit.Test;

import org.springframework.beans.factory.BeanCreationException;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.diagnostics.FailureAnalysis;
import org.springframework.boot.diagnostics.LoggingFailureAnalysisReporter;
import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.cache.support.SimpleCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link NoSuchCacheManagerFailureAnalyzer}.
 *
 * @author Dmytro Nosan
 */
public class NoSuchCacheManagerFailureAnalyzerTests {

	private final NoSuchCacheManagerFailureAnalyzer analyzer = new NoSuchCacheManagerFailureAnalyzer();

	@Test
	public void failureAnalysisForNoSuchCacheManager() {
		FailureAnalysis analysis = analyzeFailure(createFailure(
				DefaultCacheAutoConfiguration.class, "spring.cache.type=generic"));
		assertThat(analysis.getAction())
				.contains("Consider revisiting the entries above or defining "
						+ "a bean of type 'org.springframework.cache.CacheManager' in your configuration.");
		assertThat(analysis.getDescription()).contains(
				"A component required a bean of type 'org.springframework.cache.CacheManager' that could not be found.")
				.contains(
						"did not find any beans of type org.springframework.cache.Cache");
	}

	@Test
	public void failureAnalysisForNoUniqueCacheManager() {
		FailureAnalysis analysis = analyzeFailure(
				createFailure(DefaultSeveralCacheManagerConfiguration.class,
						"spring.cache.type=simple"));

		assertThat(analysis.getDescription())
				.contains("A component required a single bean, but 2 were found")
				.contains(String.format("'cacheManager' of type '%s'",
						ConcurrentMapCacheManager.class.getName()))
				.contains(String.format("'simpleCacheManager' of type '%s'",
						SimpleCacheManager.class.getName()));

		assertThat(analysis.getAction()).contains(
				"Consider marking one of the beans as @Primary, updating the consumer "
						+ "to accept multiple beans, or using @Qualifier to identify the bean that should be consumed");

	}

	private BeanCreationException createFailure(Class<?> config, String... environment) {
		try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext()) {
			this.analyzer.setBeanFactory(context.getBeanFactory());
			TestPropertyValues.of(environment).applyTo(context);
			context.register(config);
			context.refresh();
			return null;
		}
		catch (BeanCreationException ex) {
			return ex;
		}
	}

	private FailureAnalysis analyzeFailure(Exception failure) {
		FailureAnalysis analysis = this.analyzer.analyze(failure);
		if (analysis != null) {
			new LoggingFailureAnalysisReporter().report(analysis);
		}
		return analysis;
	}

	@EnableCaching
	@Configuration
	static class DefaultCacheConfiguration {

	}

	@Configuration
	static class CacheManagerConfiguration {

		@Bean
		public CacheManager simpleCacheManager() {
			return new SimpleCacheManager();
		}

	}

	@Configuration
	@ImportAutoConfiguration(CacheAutoConfiguration.class)
	@Import(DefaultCacheConfiguration.class)
	static class DefaultCacheAutoConfiguration {

	}

	@Configuration
	@Import(DefaultCacheConfiguration.class)
	@ImportAutoConfiguration({ CacheAutoConfiguration.class,
			CacheManagerConfiguration.class })
	static class DefaultSeveralCacheManagerConfiguration {

	}

}
