/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.data.cassandra;


import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.test.autoconfigure.properties.PropertyMapping;
import org.springframework.core.annotation.AliasFor;


/**
 * Annotation that can be applied to a test class to configure a cassandra keyspace.
 *
 * @author Dmytro Nosan
 * @see TestCassandraKeyspaceAutoConfiguration
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping("spring.data.cassandra")
public @interface AutoConfigureCassandraKeyspace {

	/**
	 * Sets the name of the Cassandra Keyspace to connect to.
	 *
	 * @return keyspace to use
	 */
	@AliasFor("name")
	@PropertyMapping("keyspace-name")
	String value() default "boot";

	/**
	 * Sets the name of the Cassandra Keyspace to connect to.
	 *
	 * @return keyspace to use
	 */
	@AliasFor("value")
	@PropertyMapping("keyspace-name")
	String name() default "boot";

	/**
	 * Set the {@link KeyspaceAction}.
	 *
	 * @return keyspace action to use.
	 */
	@PropertyMapping("test.keyspace-action")
	KeyspaceAction action() default KeyspaceAction.CREATE_IF_NOT_EXISTS;
}
