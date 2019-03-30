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

package org.springframework.boot.liquibase;

import java.util.Set;

import liquibase.logging.Logger;
import org.apache.commons.logging.LogFactory;
import org.junit.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Tests for SpringPackageScanClassResolver.
 *
 * @author Phillip Webb
 */
public class SpringPackageScanClassResolverTests {

	@Test
	public void testScan() {
		SpringPackageScanClassResolver resolver = new SpringPackageScanClassResolver(
				LogFactory.getLog(getClass()));
		resolver.addClassLoader(getClass().getClassLoader());
		Set<Class<?>> implementations = resolver.findImplementations(Logger.class,
				"liquibase.logging.core");
		assertThat(implementations).isNotEmpty();
	}

}
