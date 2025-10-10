/*
 * Copyright 2012-present the original author or authors.
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

package org.springframework.boot.jdbc.test.autoconfigure;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.container.ContainerImageMetadata;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.context.PropertyMapping;
import org.springframework.boot.test.context.PropertyMapping.Skip;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Annotation that can be applied to a test class to configure a test database to use
 * instead of the application-defined or auto-configured {@link DataSource}. In the case
 * of multiple {@code DataSource} beans, only the {@link Primary @Primary}
 * {@code DataSource} is considered.
 *
 * @author Phillip Webb
 * @since 4.0.0
 * @see TestDatabaseAutoConfiguration
 */
@Target({ ElementType.TYPE, ElementType.METHOD })
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.test.database")
public @interface AutoConfigureTestDatabase {

	/**
	 * Determines what type of existing DataSource bean can be replaced.
	 * @return the type of existing DataSource to replace
	 */
	@PropertyMapping(skip = Skip.ON_DEFAULT_VALUE)
	Replace replace() default Replace.NON_TEST;

	/**
	 * The type of connection to be established when {@link #replace() replacing} the
	 * DataSource. By default, will attempt to detect the connection based on the
	 * classpath.
	 * @return the type of connection to use
	 */
	EmbeddedDatabaseConnection connection() default EmbeddedDatabaseConnection.NONE;

	/**
	 * What the test database can replace.
	 */
	enum Replace {

		/**
		 * Replace the DataSource bean unless it is auto-configured and connecting to a
		 * test database. The following types of connections are considered test
		 * databases:
		 * <ul>
		 * <li>Any bean definition that includes {@link ContainerImageMetadata} (including
		 * {@code @ServiceConnection} annotated Testcontainers databases, and connections
		 * created using Docker Compose)</li>
		 * <li>Any connection configured using a {@code spring.datasource.url} backed by a
		 * {@link DynamicPropertySource @DynamicPropertySource}</li>
		 * <li>Any connection configured using a {@code spring.datasource.url} with the
		 * Testcontainers JDBC syntax</li>
		 * </ul>
		 * @since 3.4.0
		 */
		NON_TEST,

		/**
		 * Replace the DataSource bean whether it was auto-configured or manually defined.
		 */
		ANY,

		/**
		 * Only replace the DataSource if it was auto-configured.
		 */
		AUTO_CONFIGURED,

		/**
		 * Don't replace the application default DataSource.
		 */
		NONE

	}

}
