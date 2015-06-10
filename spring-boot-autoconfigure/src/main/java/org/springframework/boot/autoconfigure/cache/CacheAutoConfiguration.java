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

package org.springframework.boot.autoconfigure.cache;

import javax.annotation.PostConstruct;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfigureAfter;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration.CacheConfigurationImportSelector;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.redis.RedisAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.interceptor.CacheAspectSupport;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.ImportSelector;
import org.springframework.context.annotation.Role;
import org.springframework.core.type.AnnotationMetadata;
import org.springframework.util.Assert;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for the cache abstraction. Creates a
 * {@link CacheManager} if necessary when caching is enabled via {@link EnableCaching}.
 * <p>
 * Cache store can be auto-detected or specified explicitly via configuration.
 *
 * @author Stephane Nicoll
 * @since 1.3.0
 * @see EnableCaching
 */
@Configuration
@ConditionalOnClass(CacheManager.class)
@ConditionalOnBean(CacheAspectSupport.class)
@ConditionalOnMissingBean({ CacheManager.class, CacheResolver.class })
@EnableConfigurationProperties(CacheProperties.class)
@AutoConfigureAfter(RedisAutoConfiguration.class)
@Import(CacheConfigurationImportSelector.class)
public class CacheAutoConfiguration {

	static final String VALIDATOR_BEAN_NAME = "cacheAutoConfigurationValidator";

	@Bean
	@Role(BeanDefinition.ROLE_INFRASTRUCTURE)
	public static BeanFactoryPostProcessor cacheAutoConfigurationValidatorPostProcessor() {
		return new CacheManagerValidatorPostProcessor();
	}

	@Bean(name = VALIDATOR_BEAN_NAME)
	public CacheManagerValidator cacheAutoConfigurationValidator() {
		return new CacheManagerValidator();
	}

	/**
	 * {@link BeanFactoryPostProcessor} to ensure that the {@link CacheManagerValidator}
	 * is triggered before {@link CacheAspectSupport} but without causing early
	 * instantiation.
	 */
	static class CacheManagerValidatorPostProcessor implements BeanFactoryPostProcessor {
		@Override
		public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory)
				throws BeansException {
			for (String name : beanFactory.getBeanNamesForType(CacheAspectSupport.class)) {
				BeanDefinition definition = beanFactory.getBeanDefinition(name);
				definition.setDependsOn(append(definition.getDependsOn(),
						VALIDATOR_BEAN_NAME));
			}
		}

		private String[] append(String[] array, String value) {
			String[] result = new String[array == null ? 1 : array.length + 1];
			if (array != null) {
				System.arraycopy(array, 0, result, 0, array.length);
			}
			result[result.length - 1] = value;
			return result;
		}
	}

	/**
	 * Bean used to validate that a CacheManager exists and provide a more meaningful
	 * exception.
	 */
	static class CacheManagerValidator {

		@Autowired
		private CacheProperties cacheProperties;

		@Autowired(required = false)
		private CacheManager cacheManager;

		@PostConstruct
		public void checkHasCacheManager() {
			Assert.notNull(this.cacheManager, "No cache manager could "
					+ "be auto-configured, check your configuration (caching "
					+ "type is '" + this.cacheProperties.getType() + "')");
		}
	}

	/**
	 * {@link ImportSelector} to add {@link CacheType} configuration classes.
	 */
	static class CacheConfigurationImportSelector implements ImportSelector {

		@Override
		public String[] selectImports(AnnotationMetadata importingClassMetadata) {
			CacheType[] types = CacheType.values();
			String[] imports = new String[types.length];
			for (int i = 0; i < types.length; i++) {
				imports[i] = CacheConfigurations.getConfigurationClass(types[i]);
			}
			return imports;
		}

	}

}
