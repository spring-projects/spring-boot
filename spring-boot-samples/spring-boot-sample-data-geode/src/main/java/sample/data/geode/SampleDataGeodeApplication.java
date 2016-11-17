/*
 * Copyright 2010-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package sample.data.geode;

import org.apache.geode.cache.CacheLoader;
import org.apache.geode.cache.CacheLoaderException;
import org.apache.geode.cache.GemFireCache;
import org.apache.geode.cache.LoaderHelper;
import org.apache.geode.cache.RegionAttributes;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.data.gemfire.GemfireTemplate;
import org.springframework.data.gemfire.PartitionedRegionFactoryBean;
import org.springframework.data.gemfire.RegionAttributesFactoryBean;
import org.springframework.data.gemfire.config.annotation.PeerCacheApplication;

import sample.data.geode.config.SampleDataGeodeApplicationProperties;
import sample.data.geode.service.Calculator;
import sample.data.geode.service.factory.CalculatorFactory;

/**
 * SampleDataGeodeApplication class is a Spring Boot, Apache Geode peer cache application
 * using Apache Geode to perform and store mathematical calculations.
 *
 * @author John Blum
 * @see org.springframework.boot.SpringApplication
 * @see org.springframework.boot.autoconfigure.SpringBootApplication
 * @see org.springframework.boot.context.properties.ConfigurationProperties
 * @see org.springframework.context.ApplicationContext
 * @see org.springframework.context.annotation.Bean
 * @see org.springframework.data.gemfire.CacheFactoryBean
 * @see org.springframework.data.gemfire.GemfireTemplate
 * @see org.springframework.data.gemfire.PartitionedRegionFactoryBean
 * @see org.apache.geode.cache.Cache
 * @see org.apache.geode.cache.CacheLoader
 * @see sample.data.geode.service.Calculator
 * @see sample.data.geode.service.factory.CalculatorFactory
 * @since 1.5.0
 */
@SpringBootApplication
@PeerCacheApplication(name = "SampleDataGeodeApplication", logLevel = "config")
@EnableConfigurationProperties(SampleDataGeodeApplicationProperties.class)
@SuppressWarnings("unused")
public class SampleDataGeodeApplication {

	protected static final String CALCULATIONS_REGION_NAME = "Calculations";

	public static void main(final String[] args) {
		SpringApplication.run(SampleDataGeodeApplication.class, args);
	}

	@Autowired
	SampleDataGeodeApplicationProperties applicationProperties;

	@Bean(CALCULATIONS_REGION_NAME)
	PartitionedRegionFactoryBean<Long, Long> calculationsRegion(GemFireCache gemfireCache,
			RegionAttributes<Long, Long> calculationsRegionAttributes,
			@Qualifier("calculationsRegionLoader") CacheLoader<Long, Long> calculationsLoader) {

		PartitionedRegionFactoryBean<Long, Long> calculationsRegion =
			new PartitionedRegionFactoryBean<Long, Long>();

		calculationsRegion.setAttributes(calculationsRegionAttributes);
		calculationsRegion.setCache(gemfireCache);
		calculationsRegion.setCacheLoader(calculationsLoader);
		calculationsRegion.setClose(false);
		calculationsRegion.setPersistent(false);

		return calculationsRegion;
	}

	@Bean
	@SuppressWarnings("unchecked")
	RegionAttributesFactoryBean calculationsRegionAttributes() {
		RegionAttributesFactoryBean calculationsRegionAttributes =
			new RegionAttributesFactoryBean();

		calculationsRegionAttributes.setKeyConstraint(Long.class);
		calculationsRegionAttributes.setValueConstraint(Long.class);

		return calculationsRegionAttributes;
	}

	@Bean
	CacheLoader calculationsRegionLoader(
			@Qualifier("calculatorResolver") Calculator<Long, Long> calculator) {

		return new CacheLoader<Long, Long>() {
			@Override
			public Long load(LoaderHelper<Long, Long> loaderHelper)
					throws CacheLoaderException {

				Long operand = loaderHelper.getKey();

				return calculator.calculate(operand);
			}

			@Override
			public void close() {
			}
		};
	}

	@Bean
	GemfireTemplate calculationsRegionTemplate(GemFireCache gemfireCache) {
		return new GemfireTemplate(gemfireCache.getRegion(CALCULATIONS_REGION_NAME));
	}

	@Bean
	@SuppressWarnings("unchecked")
	Calculator<Long, Long> calculatorResolver(ApplicationContext context) {
		return context.getBean(applicationProperties.getCalculator(), Calculator.class);
	}

	@Bean
	Calculator<Long, Long> addition() {
		return CalculatorFactory.addition();
	}

	@Bean
	Calculator<Long, Long> factorial() {
		return CalculatorFactory.factorial();
	}

	@Bean
	Calculator<Long, Long> multiplication() {
		return CalculatorFactory.multiplication();
	}
}
