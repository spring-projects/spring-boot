/*
 * Copyright 2012-2014 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.boot.cli.compiler.grape;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.springframework.boot.dependency.tools.Dependency;
import org.springframework.boot.dependency.tools.ManagedDependencies;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ManagedDependenciesFactory}.
 *
 * @author Phillip Webb
 */
public class ManagedDependenciesFactoryTests {

	@Test
	public void getManagedDependencies() {
		List<Dependency> dependencyList = new ArrayList<Dependency>();
		dependencyList.add(new Dependency("g1", "a1", "1"));
		dependencyList.add(new Dependency("g1", "a2", "1"));
		ManagedDependencies dependencies = mock(ManagedDependencies.class);
		given(dependencies.iterator()).willReturn(dependencyList.iterator());
		ManagedDependenciesFactory factory = new ManagedDependenciesFactory(dependencies);
		List<org.eclipse.aether.graph.Dependency> result = factory
				.getManagedDependencies();
		assertThat(result.size(), equalTo(2));
		assertThat(result.get(0).toString(), equalTo("g1:a1:jar:1 (compile)"));
		assertThat(result.get(1).toString(), equalTo("g1:a2:jar:1 (compile)"));
	}

}
