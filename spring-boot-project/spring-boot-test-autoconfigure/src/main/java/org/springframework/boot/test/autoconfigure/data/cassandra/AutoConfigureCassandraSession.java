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
import org.springframework.boot.test.autoconfigure.properties.SkipPropertyMapping;
import org.springframework.data.cassandra.config.SchemaAction;


/**
 * Annotation that can be applied to a test class to configure a cassandra session.
 *
 * @author Dmytro Nosan
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Inherited
@ImportAutoConfiguration
@PropertyMapping(value = "spring.data.cassandra", skip = SkipPropertyMapping.ON_DEFAULT_VALUE)
public @interface AutoConfigureCassandraSession {

	/**
	 * The port to use to connect to the Cassandra host.
	 *
	 * @return port to use
	 */
	int port() default 0;

	/**
	 * Set the cluster name.
	 *
	 * @return cluster name to use.
	 */
	String clusterName() default "";

	/**
	 * Adds contact points.
	 *
	 * @return contact points.
	 */
	String[] contactPoints() default {};

	/**
	 * Set the {@link SchemaAction}.
	 *
	 * @return schema action to use.
	 */
	@PropertyMapping(skip = SkipPropertyMapping.NO)
	SchemaAction schemaAction() default SchemaAction.CREATE_IF_NOT_EXISTS;
}
