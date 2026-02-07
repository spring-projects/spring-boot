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

package org.springframework.boot.security.oauth2.server.resource.autoconfigure.servlet;

import org.springframework.security.oauth2.server.resource.introspection.OpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.SpringOpaqueTokenIntrospector;

/**
 * Callback interface for the customization of the
 * {@link SpringOpaqueTokenIntrospector.Builder} used to create the auto-configured
 * {@link OpaqueTokenIntrospector}.
 *
 * @author Vishnutheep B
 * @since 4.x.x
 */
@FunctionalInterface
public interface SpringOpaqueTokenIntrospectorBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(SpringOpaqueTokenIntrospector.Builder builder);

}
