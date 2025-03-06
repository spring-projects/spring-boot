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

package org.springframework.boot.autoconfigure.security.oauth2.resource.reactive;

import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder.JwkSetUriReactiveJwtDecoderBuilder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;

/**
 * Callback interface for the customization of the
 * {@link JwkSetUriReactiveJwtDecoderBuilder} used to create the auto-configured
 * {@link ReactiveJwtDecoder} for a JWK set URI that has been configured directly or
 * obtained through an issuer URI.
 *
 * @author Andy Wilkinson
 * @since 3.1.0
 */
@FunctionalInterface
public interface JwkSetUriReactiveJwtDecoderBuilderCustomizer {

	/**
	 * Customize the given {@code builder}.
	 * @param builder the {@code builder} to customize
	 */
	void customize(JwkSetUriReactiveJwtDecoderBuilder builder);

}
