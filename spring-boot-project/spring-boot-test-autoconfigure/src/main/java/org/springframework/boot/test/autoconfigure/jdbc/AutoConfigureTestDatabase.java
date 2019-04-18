/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.jdbc;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.sql.DataSource;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.jdbc.EmbeddedDatabaseConnection;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping;

/**
 * Annotation that can be applied to a test class to configure a test database to use
 * instead of any application defined or auto-configured {@link DataSource}.
 *
 * @author Phillip Webb
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
	 * Determines what type of existing DataSource beans can be replaced.
	 * @return the type of existing DataSource to replace
	 */
	@PropertyMapping(skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
	Replace replace() default Replace.ANY;

	/**
	 * The type of connection to be established when {@link #replace() replacing} the data
	 * source. By default will attempt to detect the connection based on the classpath.
	 * @return the type of connection to use
	 */
	EmbeddedDatabaseConnection connection() default EmbeddedDatabaseConnection.NONE;

	/**
	 * What the test database can replace.
	 */
	enum Replace {

		/**
		 * Replace any DataSource bean (auto-configured or manually defined).
		 */
		ANY,

		/**
		 * Only replace auto-configured DataSource.
		 */
		AUTO_CONFIGURED,

		/**
		 * Don't replace the application default DataSource.
		 */
		NONE

	}

}
