/*
 * Copyright 2012-2024 the original author or authors.
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

package org.springframework.boot.testcontainers.lifecycle;

import org.testcontainers.containers.Container;

import org.springframework.context.ApplicationEvent;
import org.springframework.test.context.DynamicPropertyRegistrar;

/**
 * Event published just before a Testcontainers {@link Container} is used.
 *
 * @author Andy Wilkinson
 * @since 3.2.6
 * @deprecated since 3.4.0 for removal in 3.6.0 in favor of property registration using a
 * {@link DynamicPropertyRegistrar} bean that injects the {@link Container} from which the
 * properties will be sourced.
 */
@Deprecated(since = "3.4.0", forRemoval = true)
public class BeforeTestcontainerUsedEvent extends ApplicationEvent {

	public BeforeTestcontainerUsedEvent(Object source) {
		super(source);
	}

}
