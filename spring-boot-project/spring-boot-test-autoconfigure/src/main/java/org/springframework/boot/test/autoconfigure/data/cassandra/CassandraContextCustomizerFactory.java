/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.cassandra;

import java.util.List;

import org.springframework.boot.test.util.TestPropertyValues;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.test.context.ContextConfigurationAttributes;
import org.springframework.test.context.ContextCustomizer;
import org.springframework.test.context.ContextCustomizerFactory;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.util.StringUtils;

/**
 * The main goal of this that {@link org.springframework.data.cassandra.config.CassandraSessionFactoryBean CassandraSessionFactoryBean} is not lazy and tries
 * to create session instantly, hence we should be sure that is the {@link DataCassandraTest DataCassandraTest }, otherwise
 * others test will be failed.
 *
 * @author Dmytro Nosan
 */
class CassandraContextCustomizerFactory implements ContextCustomizerFactory {

	@Override
	public ContextCustomizer createContextCustomizer(Class<?> testClass, List<ContextConfigurationAttributes> configurationAttributes) {
		AutoConfigureDataCassandra annotation = AnnotatedElementUtils.findMergedAnnotation(testClass, AutoConfigureDataCassandra.class);
		if (annotation == null) {
			return new DisableCassandraAutoConfigurations();
		}
		return null;
	}

	/**
	 * {@link ContextCustomizer} to disable Cassandra AutoConfigurations.
	 */
	private static class DisableCassandraAutoConfigurations implements ContextCustomizer {

		@Override
		public void customizeContext(ConfigurableApplicationContext context, MergedContextConfiguration mergedConfig) {

			List<String> factoryNames = SpringFactoriesLoader.loadFactoryNames(AutoConfigureDataCassandra.class,
					context.getClassLoader());

			TestPropertyValues
					.of("spring.autoconfigure.exclude=" + StringUtils.collectionToDelimitedString(factoryNames, ","))
					.applyTo(context);

		}

		@Override
		public int hashCode() {
			return getClass().hashCode();
		}

		@Override
		public boolean equals(Object obj) {
			return (obj != null && obj.getClass() == getClass());
		}

	}
}
