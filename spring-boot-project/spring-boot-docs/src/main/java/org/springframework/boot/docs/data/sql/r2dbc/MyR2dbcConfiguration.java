/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.data.sql.r2dbc;

import io.r2dbc.spi.ConnectionFactoryOptions;

import org.springframework.boot.autoconfigure.r2dbc.ConnectionFactoryOptionsBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MyR2dbcConfiguration class.
 */
@Configuration(proxyBeanMethods = false)
public class MyR2dbcConfiguration {

	/**
     * Customizes the connection factory options builder to set the port to 5432.
     *
     * @return the connection factory options builder customizer
     */
    @Bean
	public ConnectionFactoryOptionsBuilderCustomizer connectionFactoryPortCustomizer() {
		return (builder) -> builder.option(ConnectionFactoryOptions.PORT, 5432);
	}

}
