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

package org.springframework.boot.autoconfigure.data.geode;

import org.apache.geode.cache.Cache;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.RegionAttributes;
import org.apache.geode.cache.client.ClientRegionShortcut;

import org.junit.After;
import org.junit.Test;

import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.boot.autoconfigure.TestAutoConfigurationPackage;
import org.springframework.boot.autoconfigure.data.alt.geode.CityGeodeRepository;
import org.springframework.boot.autoconfigure.data.empty.EmptyDataPackage;
import org.springframework.boot.autoconfigure.data.geode.city.City;
import org.springframework.boot.autoconfigure.data.geode.city.CityRepository;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.gemfire.LocalRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.client.ClientRegionFactoryBean;
import org.springframework.data.gemfire.config.annotation.ClientCacheApplication;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;
import org.springframework.data.gemfire.repository.GemfireRepository;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Integration test for auto-configuring Spring Data Geode Repository with Spring Boot.
 *
 * @author John Blum
 * @since 1.5.0
 */
public class GeodeRepositoriesAutoConfigurationTests {

	private AnnotationConfigApplicationContext applicationContext;

	@After
	public void tearDown() {
		this.applicationContext.close();
	}

	@Test
	public void clientCacheWithDefaultRepositoryConfigurationIsSuccessful() {
		prepareApplicationContext(ClientCacheGemFireConfiguration.class);

		assertThat(this.applicationContext.getBean(CityRepository.class)).isNotNull();
	}

	@Test
	public void peerCacheWithDefaultRepositoryConfigurationIsSuccessful() {
		prepareApplicationContext(PeerCacheGemFireConfiguration.class);

		assertThat(this.applicationContext.getBean(CityRepository.class)).isNotNull();
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void noRepositoryConfiguration() {
		prepareApplicationContext(EmptyConfiguration.class, PeerCacheGemFireConfiguration.class);

		assertThat(this.applicationContext.getBean(Cache.class)).isNotNull();

		this.applicationContext.getBean(GemfireRepository.class);
	}

	@Test(expected = NoSuchBeanDefinitionException.class)
	public void doesNotTriggerDefaultRepositoryDetectionIfCustomized() {
		prepareApplicationContext(CustomizedConfiguration.class, PeerCacheGemFireConfiguration.class);

		assertThat(this.applicationContext.getBean(CityGeodeRepository.class)).isNotNull();

		this.applicationContext.getBean(CityRepository.class);
	}

	protected void prepareApplicationContext(Class<?>... annotatedClasses) {
		this.applicationContext = new AnnotationConfigApplicationContext();
		this.applicationContext.register(annotatedClasses);
		this.applicationContext.register(GeodeRepositoriesAutoConfiguration.class);
		this.applicationContext.refresh();
	}

	@SuppressWarnings("unused")
	static abstract class AbstractBaseGemFireConfiguration {

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

	@SuppressWarnings("unused")
	@TestAutoConfigurationPackage(City.class)
	@ClientCacheApplication(name = "GeodeClientCacheApplication", logLevel = "warning")
	static class ClientCacheGemFireConfiguration extends AbstractBaseGemFireConfiguration {

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

	@SuppressWarnings("unused")
	@TestAutoConfigurationPackage(City.class)
	@PeerCacheApplication(name = "GeodePeerCacheApplication", logLevel = "warning")
	static class PeerCacheGemFireConfiguration extends AbstractBaseGemFireConfiguration {

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
	@TestAutoConfigurationPackage(GeodeRepositoriesAutoConfigurationTests.class)
	@EnableGemfireRepositories(basePackageClasses = CityGeodeRepository.class)
	static class CustomizedConfiguration {

	}
}
