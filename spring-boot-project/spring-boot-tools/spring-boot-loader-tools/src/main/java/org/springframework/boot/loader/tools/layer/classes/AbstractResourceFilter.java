/*
 * Copyright 2012-2020 the original author or authors.
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

package org.springframework.boot.loader.tools.layer.classes;

import java.util.ArrayList;
import java.util.List;

/**
 * Abstract base class for {@link ResourceFilter} implementations.
 *
 * @author Madhura Bhave
 * @since 2.3.0
 */
public abstract class AbstractResourceFilter implements ResourceFilter {

	private final List<String> includes = new ArrayList<>();

	private final List<String> excludes = new ArrayList<>();

	public AbstractResourceFilter(List<String> includes, List<String> excludes) {
		this.includes.addAll(includes);
		this.excludes.addAll(excludes);
	}

	@Override
	public boolean isResourceIncluded(String resourceName) {
		return isMatch(resourceName, this.includes);
	}

	@Override
	public boolean isResourceExcluded(String resourceName) {
		return isMatch(resourceName, this.excludes);
	}

	protected abstract boolean isMatch(String resourceName, List<String> toMatch);

}
