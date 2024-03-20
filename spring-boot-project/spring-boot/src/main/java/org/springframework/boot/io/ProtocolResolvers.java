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

package org.springframework.boot.io;

import java.util.List;

import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.ProtocolResolver;
import org.springframework.core.io.support.SpringFactoriesLoader;
import org.springframework.util.Assert;

/**
 * {@link ProtocolResolver} implementations that are loaded from a
 * {@code spring.factories} file.
 *
 * @author Scott Frederick
 */
final class ProtocolResolvers {

	private ProtocolResolvers() {
	}

	static <T extends DefaultResourceLoader> void applyTo(T resourceLoader) {
		Assert.notNull(resourceLoader, "ResourceLoader must not be null");
		SpringFactoriesLoader loader = SpringFactoriesLoader
			.forDefaultResourceLocation(resourceLoader.getClassLoader());
		List<ProtocolResolver> resolvers = loader.load(ProtocolResolver.class);
		resourceLoader.getProtocolResolvers().addAll(resolvers);
	}

}
