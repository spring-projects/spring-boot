/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.autoconfigure;

import org.springframework.boot.context.condition.ConditionalOnMissingBean;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * {@link EnableAutoConfiguration Auto-configuration} for
 * {@link PropertySourcesPlaceholderConfigurer}.
 * 
 * @author Phillip Webb
 * @author Dave Syer
 */
@Configuration
@Order(Ordered.HIGHEST_PRECEDENCE)
public class PropertyPlaceholderAutoConfiguration {

	@Bean
	@ConditionalOnMissingBean
	public static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer(
			ApplicationContext context) {
		return new PropertySourcesPlaceholderConfigurer();
	}

}
