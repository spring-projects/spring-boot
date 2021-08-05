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

import java.lang.reflect.Method;
import java.util.function.Function;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;

import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.util.StringUtils;

/**
 * Default {@link RepositoryTagsProvider} implementation.
 *
 * @author Phillip Webb
 * @since 2.5.0
 */
public class DefaultRepositoryTagsProvider implements RepositoryTagsProvider {

	private static final Tag EXCEPTION_NONE = Tag.of("exception", "None");

	@Override
	public Iterable<Tag> repositoryTags(RepositoryMethodInvocation invocation) {
		Tags tags = Tags.empty();
		tags = and(tags, invocation.getRepositoryInterface(), "repository", this::getSimpleClassName);
		tags = and(tags, invocation.getMethod(), "method", Method::getName);
		tags = and(tags, invocation.getResult().getState(), "state", State::name);
		tags = and(tags, invocation.getResult().getError(), "exception", this::getExceptionName, EXCEPTION_NONE);
		return tags;
	}

	private <T> Tags and(Tags tags, T instance, String key, Function<T, String> value) {
		return and(tags, instance, key, value, null);
	}

	private <T> Tags and(Tags tags, T instance, String key, Function<T, String> value, Tag fallback) {
		if (instance != null) {
			return tags.and(key, value.apply(instance));
		}
		return (fallback != null) ? tags.and(fallback) : tags;
	}

	private String getExceptionName(Throwable error) {
		return getSimpleClassName(error.getClass());
	}

	private String getSimpleClassName(Class<?> type) {
		String simpleName = type.getSimpleName();
		return (!StringUtils.hasText(simpleName)) ? type.getName() : simpleName;
	}

}
