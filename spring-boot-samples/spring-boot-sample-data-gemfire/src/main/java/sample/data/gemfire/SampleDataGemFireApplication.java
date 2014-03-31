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

package sample.data.gemfire;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportResource;
import org.springframework.data.gemfire.repository.config.EnableGemfireRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * The GemstoneAppConfiguration class for allowing Spring Boot to pickup additional application Spring configuration
 * meta-data for GemFire, which must be specified in Spring Data GemFire's XML namespace.
 * <p/>
 * @author John Blum
 * @see org.springframework.boot.autoconfigure.EnableAutoConfiguration
 * @see org.springframework.context.annotation.ComponentScan
 * @see org.springframework.context.annotation.Configuration
 * @see org.springframework.context.annotation.ImportResource
 * @see org.springframework.data.gemfire.repository.config.EnableGemfireRepositories
 * @see org.springframework.transaction.annotation.EnableTransactionManagement
 * @since 1.0.0
 */
@Configuration
@ImportResource("/spring-data-gemfire-cache.xml")
@ComponentScan
@EnableAutoConfiguration
@EnableGemfireRepositories
@EnableTransactionManagement
@SuppressWarnings("unused")
public class SampleDataGemFireApplication {

	public static void main(final String[] args) {
		SpringApplication.run(SampleDataGemFireApplication.class, args);
	}

}
