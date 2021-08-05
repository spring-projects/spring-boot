/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.actuate.metrics.data;

import io.micrometer.core.instrument.Tag;

import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;

/**
 * Provides {@link Tag Tags} for Spring Data {@link RepositoryMethodInvocation Repository
 * invocations}.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
@FunctionalInterface
public interface RepositoryTagsProvider {

	/**
	 * Provides tags to be associated with metrics for the given {@code invocation}.
	 * @param invocation the repository invocation
	 * @return tags to associate with metrics for the invocation
	 */
	Iterable<Tag> repositoryTags(RepositoryMethodInvocation invocation);

}
