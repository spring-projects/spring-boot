/*
 * Copyright 2012-2018 the original author or authors.
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

package org.springframework.boot.actuate.web.mappings;

import java.util.List;

import org.springframework.context.ApplicationContext;

/**
 * A {@link MappingDescriptionProvider} provides a {@link List} of mapping descriptions
 * via implementation-specific introspection of an application context.
 *
 * @author Andy Wilkinson
 * @since 2.0.0
 */
public interface MappingDescriptionProvider {

	/**
	 * Returns the name of the mappings described by this provider.
	 * @return the name of the mappings
	 */
	String getMappingName();

	/**
	 * Produce the descriptions of the mappings identified by this provider in the given
	 * {@code context}.
	 * @param context the application context to introspect
	 * @return the mapping descriptions
	 */
	Object describeMappings(ApplicationContext context);

}
