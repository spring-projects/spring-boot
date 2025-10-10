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

package org.springframework.boot.devtools.classpath;

import org.jspecify.annotations.Nullable;

import org.springframework.boot.devtools.filewatch.ChangedFile;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;

/**
 * Ant style pattern based {@link ClassPathRestartStrategy}.
 *
 * @author Phillip Webb
 * @since 1.3.0
 * @see ClassPathRestartStrategy
 */
public class PatternClassPathRestartStrategy implements ClassPathRestartStrategy {

	private final AntPathMatcher matcher = new AntPathMatcher();

	private final String[] excludePatterns;

	public PatternClassPathRestartStrategy(String[] excludePatterns) {
		this.excludePatterns = excludePatterns;
	}

	public PatternClassPathRestartStrategy(@Nullable String excludePatterns) {
		this(StringUtils.commaDelimitedListToStringArray(excludePatterns));
	}

	@Override
	public boolean isRestartRequired(ChangedFile file) {
		for (String pattern : this.excludePatterns) {
			if (this.matcher.match(pattern, file.getRelativeName())) {
				return false;
			}
		}
		return true;
	}

}
