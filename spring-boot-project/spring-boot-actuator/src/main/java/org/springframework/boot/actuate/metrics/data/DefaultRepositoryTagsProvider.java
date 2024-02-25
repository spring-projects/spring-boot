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

	/**
	 * Returns the tags associated with the repository method invocation.
	 * @param invocation the repository method invocation
	 * @return an iterable of tags
	 */
	@Override
	public Iterable<Tag> repositoryTags(RepositoryMethodInvocation invocation) {
		Tags tags = Tags.empty();
		tags = and(tags, invocation.getRepositoryInterface(), "repository", this::getSimpleClassName);
		tags = and(tags, invocation.getMethod(), "method", Method::getName);
		tags = and(tags, invocation.getResult().getState(), "state", State::name);
		tags = and(tags, invocation.getResult().getError(), "exception", this::getExceptionName, EXCEPTION_NONE);
		return tags;
	}

	/**
	 * Combines the given tags with the specified instance, key, and value using the
	 * logical AND operator.
	 * @param <T> the type of the instance
	 * @param tags the tags to be combined
	 * @param instance the instance to be tagged
	 * @param key the key of the tag
	 * @param value the function to extract the value of the tag from the instance
	 * @return the combined tags
	 */
	private <T> Tags and(Tags tags, T instance, String key, Function<T, String> value) {
		return and(tags, instance, key, value, null);
	}

	/**
	 * Combines the given tags with the specified instance, key, value, and fallback tag.
	 * @param <T> the type of the instance
	 * @param tags the original tags
	 * @param instance the instance to retrieve the value from
	 * @param key the key for the tag
	 * @param value the function to retrieve the value from the instance
	 * @param fallback the fallback tag if the instance is null
	 * @return the combined tags
	 */
	private <T> Tags and(Tags tags, T instance, String key, Function<T, String> value, Tag fallback) {
		if (instance != null) {
			return tags.and(key, value.apply(instance));
		}
		return (fallback != null) ? tags.and(fallback) : tags;
	}

	/**
	 * Returns the name of the exception thrown by the given error.
	 * @param error the error for which to retrieve the exception name
	 * @return the name of the exception
	 */
	private String getExceptionName(Throwable error) {
		return getSimpleClassName(error.getClass());
	}

	/**
	 * Returns the simple class name of the given type. If the simple name is empty, the
	 * fully qualified name of the type is returned.
	 * @param type the type for which to get the simple class name
	 * @return the simple class name of the given type
	 */
	private String getSimpleClassName(Class<?> type) {
		String simpleName = type.getSimpleName();
		return (!StringUtils.hasText(simpleName)) ? type.getName() : simpleName;
	}

}
