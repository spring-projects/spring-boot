/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.testcontainers.service.connection;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.testcontainers.containers.Container;
import org.testcontainers.utility.DockerImageName;

import org.springframework.boot.autoconfigure.service.connection.ConnectionDetails;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.AliasFor;

/**
 * Annotation used to indicate that a field or method is a
 * {@link ContainerConnectionSource} which provides a service that can be connected to.
 *
 * @author Moritz Halbritter
 * @author Andy Wilkinson
 * @author Phillip Webb
 * @since 3.1.0
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE })
public @interface ServiceConnection {

	/**
	 * The name of the service being connected to. Container names are used to determine
	 * the connection details that should be created when a technology-specific
	 * {@link Container} subclass is not available.
	 * <p>
	 * If not specified, and if the {@link Container} instance is available, the
	 * {@link DockerImageName#getRepository() repository} part of the
	 * {@link Container#getDockerImageName() docker image name} will be used. Note that
	 * {@link Container} instances are <em>not</em> available early enough when the
	 * container is defined as a {@link Bean @Bean} method. All
	 * {@link ServiceConnection @ServiceConnection} {@link Bean @Bean} methods that need
	 * to match on the connection name <em>must</em> declare this attribute.
	 * <p>
	 * This attribute is an alias for {@link #name()}.
	 * @return the name of the service
	 * @see #name()
	 */
	@AliasFor("name")
	String value() default "";

	/**
	 * The name of the service being connected to. Container names are used to determine
	 * the connection details that should be created when a technology-specific
	 * {@link Container} subclass is not available.
	 * <p>
	 * If not specified, and if the {@link Container} instance is available, the
	 * {@link DockerImageName#getRepository() repository} part of the
	 * {@link Container#getDockerImageName() docker image name} will be used. Note that
	 * {@link Container} instances are <em>not</em> available early enough when the
	 * container is defined as a {@link Bean @Bean} method. All
	 * {@link ServiceConnection @ServiceConnection} {@link Bean @Bean} methods that need
	 * to match on the connection name <em>must</em> declare this attribute.
	 * <p>
	 * This attribute is an alias for {@link #value()}.
	 * @return the name of the service
	 * @see #value()
	 */
	@AliasFor("value")
	String name() default "";

	/**
	 * A restriction to types of {@link ConnectionDetails} that can be created from this
	 * connection. The default value does not restrict the types that can be created.
	 * @return the connection detail types that can be created to establish the connection
	 */
	Class<? extends ConnectionDetails>[] type() default {};

}
