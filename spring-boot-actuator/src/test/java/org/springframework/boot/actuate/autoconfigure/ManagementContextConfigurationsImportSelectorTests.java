/*
 * Copyright 2012-2017 the original author or authors.
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

package org.springframework.boot.actuate.autoconfigure;

import java.util.Arrays;
import java.util.List;

import org.junit.Test;

import org.springframework.core.annotation.Order;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for {@link ManagementContextConfigurationsImportSelector}.
 *
 * @author Phillip Webb
 * @author Andy Wilkinson
 */
public class ManagementContextConfigurationsImportSelectorTests {

	@Test
	public void selectImportsShouldOrderResult() throws Exception {
		String[] imports = new TestManagementContextConfigurationsImportSelector()
				.selectImports(null);
		assertThat(imports).containsExactly(A.class.getName(), B.class.getName(),
				C.class.getName(), D.class.getName());
	}

	private static class TestManagementContextConfigurationsImportSelector
			extends ManagementContextConfigurationsImportSelector {

		@Override
		protected List<String> loadFactoryNames() {
			return Arrays.asList(C.class.getName(), A.class.getName(), D.class.getName(),
					B.class.getName());
		}

	}

	@Order(1)
	private static class A {

	}

	@Order(2)
	private static class B {

	}

	@Order(3)
	private static class C {

	}

	static class D {

	}

}
