/*
 * Copyright 2012-2022 the original author or authors.
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

import java.io.IOException;
import java.lang.reflect.Method;

import io.micrometer.core.instrument.Tag;
import org.junit.jupiter.api.Test;

import org.springframework.data.repository.Repository;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocation;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult;
import org.springframework.data.repository.core.support.RepositoryMethodInvocationListener.RepositoryMethodInvocationResult.State;
import org.springframework.util.ReflectionUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link DefaultRepositoryTagsProvider}.
 *
 * @author Phillip Webb
 */
class DefaultRepositoryTagsProviderTests {

	private final DefaultRepositoryTagsProvider provider = new DefaultRepositoryTagsProvider();

	@Test
	void repositoryTagsIncludesRepository() {
		RepositoryMethodInvocation invocation = createInvocation();
		Iterable<Tag> tags = this.provider.repositoryTags(invocation);
		assertThat(tags).contains(Tag.of("repository", "ExampleRepository"));
	}

	@Test
	void repositoryTagsIncludesMethod() {
		RepositoryMethodInvocation invocation = createInvocation();
		Iterable<Tag> tags = this.provider.repositoryTags(invocation);
		assertThat(tags).contains(Tag.of("method", "findById"));
	}

	@Test
	void repositoryTagsIncludesState() {
		RepositoryMethodInvocation invocation = createInvocation();
		Iterable<Tag> tags = this.provider.repositoryTags(invocation);
		assertThat(tags).contains(Tag.of("state", "SUCCESS"));
	}

	@Test
	void repositoryTagsIncludesException() {
		RepositoryMethodInvocation invocation = createInvocation(new IOException());
		Iterable<Tag> tags = this.provider.repositoryTags(invocation);
		assertThat(tags).contains(Tag.of("exception", "IOException"));
	}

	@Test
	void repositoryTagsWhenNoExceptionIncludesExceptionTagWithNone() {
		RepositoryMethodInvocation invocation = createInvocation();
		Iterable<Tag> tags = this.provider.repositoryTags(invocation);
		assertThat(tags).contains(Tag.of("exception", "None"));
	}

	private RepositoryMethodInvocation createInvocation() {
		return createInvocation(null);
	}

	private RepositoryMethodInvocation createInvocation(Throwable error) {
		Class<?> repositoryInterface = ExampleRepository.class;
		Method method = ReflectionUtils.findMethod(repositoryInterface, "findById", long.class);
		RepositoryMethodInvocationResult result = mock(RepositoryMethodInvocationResult.class);
		given(result.getState()).willReturn((error != null) ? State.ERROR : State.SUCCESS);
		given(result.getError()).willReturn(error);
		return new RepositoryMethodInvocation(repositoryInterface, method, result, 0);
	}

	interface ExampleRepository extends Repository<Example, Long> {

		Example findById(long id);

	}

	static class Example {

	}

}
