/*
 * Copyright 2012-2019 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure.web;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.springframework.context.annotation.Configuration;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;

/**
 * Specialized {@link Configuration @Configuration} class that defines configuration
 * specific for the management context. Configurations should be registered in
 * {@code /META-INF/spring.factories} under the
 * {@code org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration}
 * key.
 * <p>
 * {@code ManagementContextConfiguration} classes can be ordered using {@link Order}.
 * Ordering by implementing {@link Ordered} is not supported and will have no effect.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 * @since 2.0.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Configuration
public @interface ManagementContextConfiguration {

	/**
	 * Specifies the type of management context that is required for this configuration to
	 * be applied.
	 * @return the required management context type
	 * @since 2.0.0
	 */
	ManagementContextType value() default ManagementContextType.ANY;

}
