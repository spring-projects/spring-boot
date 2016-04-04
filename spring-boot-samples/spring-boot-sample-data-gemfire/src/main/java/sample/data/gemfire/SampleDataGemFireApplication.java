/*
 * Copyright 2010-2013 the original author or authors.
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

package sample.data.gemfire;

import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.CacheFactoryBean;
import org.springframework.data.gemfire.GemfireTransactionManager;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.ReplicatedRegionFactoryBean;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import com.gemstone.gemfire.cache.Cache;
import com.gemstone.gemfire.cache.RegionAttributes;

import sample.data.gemfire.config.SampleDataGemFireApplicationProperties;
import sample.data.gemfire.domain.Gemstone;

/**
 * The GemstoneAppConfiguration class for allowing Spring Boot to pick up additional
 * application Spring configuration meta-data for GemFire, which must be specified in
 * Spring Data GemFire's XML namespace.
 *
 * @author John Blum
 */
@SpringBootApplication
@EnableGemfireRepositories
@EnableTransactionManagement
@EnableConfigurationProperties(SampleDataGemFireApplicationProperties.class)
@SuppressWarnings("unused")
public class SampleDataGemFireApplication {

	protected static final String GEMSTONES_REGION_NAME = "Gemstones";

	public static void main(final String[] args) {
		SpringApplication.run(SampleDataGemFireApplication.class, args);
	}

	@Autowired
	SampleDataGemFireApplicationProperties applicationProperties;

	Properties gemfireProperties() {
		Properties gemfireProperties = new Properties();

		gemfireProperties.setProperty("name", SampleDataGemFireApplication.class.getSimpleName());
		gemfireProperties.setProperty("mcast-port", "0");
		gemfireProperties.setProperty("locators", "");
		gemfireProperties.setProperty("log-level", applicationProperties.getLogLevel());

		return gemfireProperties;
	}

	@Bean
	CacheFactoryBean gemfireCache() {
		CacheFactoryBean gemfireCache = new CacheFactoryBean();

		gemfireCache.setClose(true);
		gemfireCache.setProperties(gemfireProperties());

		return gemfireCache;
	}

	@Bean(name = GEMSTONES_REGION_NAME)
	ReplicatedRegionFactoryBean<Long, Gemstone> gemstonesRegion(Cache gemfireCache,
			RegionAttributes<Long, Gemstone> gemstonesRegionAttributes) {

		ReplicatedRegionFactoryBean<Long, Gemstone> gemstonesRegion =
			new ReplicatedRegionFactoryBean<Long, Gemstone>();

		gemstonesRegion.setAttributes(gemstonesRegionAttributes);
		gemstonesRegion.setClose(false);
		gemstonesRegion.setCache(gemfireCache);
		gemstonesRegion.setName(GEMSTONES_REGION_NAME);
		gemstonesRegion.setPersistent(false);

		return gemstonesRegion;
	}

	@Bean
	@SuppressWarnings("unchecked")
	RegionAttributesFactoryBean gemstonesRegionAttributes() {
		RegionAttributesFactoryBean gemstonesRegionAttributes =
			new RegionAttributesFactoryBean();

		gemstonesRegionAttributes.setKeyConstraint(Long.class);
		gemstonesRegionAttributes.setValueConstraint(Gemstone.class);

		return gemstonesRegionAttributes;
	}

	@Bean
	GemfireTransactionManager gemfireTransactionManager(Cache gemfireCache) {
		return new GemfireTransactionManager(gemfireCache);
	}

}
