/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.autoconfigure.web;

import java.util.Set;

import org.springframework.util.PropertyPlaceholderHelper;

/**
 * {@link PropertyPlaceholderHelper} that doesn't allow recursive resolution.
 *
 * @author Phillip Webb
 */
class NonRecursivePropertyPlaceholderHelper extends PropertyPlaceholderHelper {

	NonRecursivePropertyPlaceholderHelper(String placeholderPrefix,
			String placeholderSuffix) {
		super(placeholderPrefix, placeholderSuffix);
	}

	@Override
	protected String parseStringValue(String strVal,
			PlaceholderResolver placeholderResolver, Set<String> visitedPlaceholders) {
		return super.parseStringValue(strVal,
				new NonRecursivePlaceholderResolver(placeholderResolver),
				visitedPlaceholders);
	}

	private static class NonRecursivePlaceholderResolver implements PlaceholderResolver {

		private final PlaceholderResolver resolver;

		NonRecursivePlaceholderResolver(PlaceholderResolver resolver) {
			this.resolver = resolver;
		}

		@Override
		public String resolvePlaceholder(String placeholderName) {
			if (this.resolver instanceof NonRecursivePlaceholderResolver) {
				return null;
			}
			return this.resolver.resolvePlaceholder(placeholderName);
		}

	}

}
