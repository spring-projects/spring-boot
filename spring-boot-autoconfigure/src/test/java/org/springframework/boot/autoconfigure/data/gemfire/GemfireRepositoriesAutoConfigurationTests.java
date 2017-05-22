/*
 * Copyright 2011-2016 the original author or authors.
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

package org.springframework.boot.autoconfigure.data.gemfire;

import java.util.Properties;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.GemFireCache;
import com.gemstone.gemfire.cache.RegionAttributes;
import com.gemstone.gemfire.cache.client.ClientRegionShortcut;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.alt.gemfire.CityGemFireRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.gemfire.city.City;
import org.springframework.boot.autoconfigure.data.gemfire.city.CityRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.client.ClientCacheFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The GemfireRepositoriesAutoConfigurationTests class...
 *
 * @author John Blum
 * @since 1.0.0
 */
public class GemfireRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext applicationContext;

	@After
	public void tearDown() {
		applicationContext.close();
	}

	@Test
	public void clientCacheWithDefaultRepositoryConfigurationIsSuccessful() {
		prepareApplicationContext(ClientCacheGemFireConfiguration.class);

		assertThat(applicationContext.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	public void peerCacheWithDefaultRepositoryConfigurationIsSuccessful() {
		prepareApplicationContext(PeerCacheGemFireConfiguration.class);

		assertThat(applicationContext.getBean(CityRepository.class)).isNotNull();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void noRepositoryConfiguration() {
		prepareApplicationContext(EmptyConfiguration.class, PeerCacheGemFireConfiguration.class);

		assertThat(applicationContext.getBean(Cache.class)).isNotNull();

		applicationContext.getBean(GemfireRepository.class);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		prepareApplicationContext(CustomizedConfiguration.class, PeerCacheGemFireConfiguration.class);

		assertThat(applicationContext.getBean(CityGemFireRepository.class)).isNotNull();

		applicationContext.getBean(CityRepository.class);
	}

	protected void prepareApplicationContext(Class<?>... annotatedClasses) {
		applicationContext = new AnnotationConfigApplicationContext();
		applicationContext.register(annotatedClasses);
		applicationContext.register(GemFireRepositoriesAutoConfiguration.class);
		applicationContext.refresh();
	}

	@SuppressWarnings("unused")
	static abstract class AbstractBaseGemFireConfiguration {

		String applicationName() {
			return GemfireRepositoriesAutoConfigurationTests.class.getSimpleName();
		}

		String logLevel() {
			return System.getProperty("gemfire.log-level", "warning");
		}

		Properties gemfireProperties() {
			Properties gemfireProperties = new Properties();

			gemfireProperties.setProperty("name", applicationName());
			gemfireProperties.setProperty("mcast-port", "0");
			gemfireProperties.setProperty("log-level", logLevel());

			return gemfireProperties;
		}

		@Bean
		@SuppressWarnings("unchecked")
		RegionAttributesFactoryBean citiesRegionAttributes() {
			RegionAttributesFactoryBean citiesRegionAttributes =
				new RegionAttributesFactoryBean();

			citiesRegionAttributes.setKeyConstraint(Long.class);
			citiesRegionAttributes.setValueConstraint(City.class);

			return citiesRegionAttributes;
		}
	}

	@Configuration
	@SuppressWarnings("unused")
	@TestAutoConfigurationPackage(City.class)
	static class ClientCacheGemFireConfiguration extends AbstractBaseGemFireConfiguration {

		@Bean
		ClientCacheFactoryBean gemfireCache() {
			ClientCacheFactoryBean gemfireCache = new ClientCacheFactoryBean();

			gemfireCache.setClose(true);
			gemfireCache.setProperties(gemfireProperties());

			return gemfireCache;
		}

		@Bean(name = "Cities")
		ClientRegionFactoryBean<Long, City> citiesRegion(GemFireCache gemfireCache,
			RegionAttributes<Long, City> citiesRegionAttributes) {

			ClientRegionFactoryBean<Long, City> citiesRegion =
				new ClientRegionFactoryBean<Long, City>();

			citiesRegion.setAttributes(citiesRegionAttributes);
			citiesRegion.setCache(gemfireCache);
			citiesRegion.setClose(false);
			citiesRegion.setShortcut(ClientRegionShortcut.LOCAL);

			return citiesRegion;
		}
	}

	@Configuration
	@SuppressWarnings("unused")
	@TestAutoConfigurationPackage(City.class)
	static class PeerCacheGemFireConfiguration extends AbstractBaseGemFireConfiguration {

		@Bean
		CacheFactoryBean gemfireCache() {
			CacheFactoryBean gemfireCache = new CacheFactoryBean();

			gemfireCache.setClose(true);
			gemfireCache.setProperties(gemfireProperties());

			return gemfireCache;
		}

		@Bean(name = "Cities")
		LocalRegionFactoryBean<Long, City> citiesRegion(GemFireCache gemfireCache,
			RegionAttributes<Long, City> citiesRegionAttributes) {

			LocalRegionFactoryBean<Long, City> citiesRegion =
				new LocalRegionFactoryBean<Long, City>();

			citiesRegion.setAttributes(citiesRegionAttributes);
			citiesRegion.setCache(gemfireCache);
			citiesRegion.setClose(false);
			citiesRegion.setPersistent(false);

			return citiesRegion;
		}
	}

	@Configuration
	@TestAutoConfigurationPackage(EmptyDataPackage.class)
	protected static class EmptyConfiguration {

	}

	@Configuration
	@TestAutoConfigurationPackage(GemfireRepositoriesAutoConfigurationTests.class)
	@EnableGemfireRepositories(basePackageClasses = CityGemFireRepository.class)
	static class CustomizedConfiguration {

	}
}
