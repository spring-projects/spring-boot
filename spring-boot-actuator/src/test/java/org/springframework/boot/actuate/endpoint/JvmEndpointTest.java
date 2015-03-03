/*
 * Copyright 2012-2015 the original author or authors.
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

package org.springframework.boot.actuate.endpoint;

import org.junit.Test;

import java.util.Collections;
import java.util.Map;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

/**
 * @author Jakub Kubrynski
 */
public class JvmEndpointTest {

	@Test
	public void shouldReturnNonDefaultVmFlags() throws Exception {
		//given
		JvmEndpoint jvmEndpoint = new JvmEndpoint();
		jvmEndpoint.setBeanClassLoader(ClassLoader.getSystemClassLoader());

		//when
		Map<String, String> result = jvmEndpoint.invoke();
		String nonDefaultFlags = result.get(JvmEndpoint.NON_DEFAULT_FLAGS);

		//then
		assertThat(nonDefaultFlags, notNullValue());
		assertThat(nonDefaultFlags, not(equalTo(JvmEndpoint.NOT_FOUND_MESSAGE)));
	}

	@Test
	public void shouldReturnParticularFlagValue() throws Exception {
		//given
		JvmEndpoint jvmEndpoint = new JvmEndpoint(Collections.singletonList("InitialHeapSize"));
		jvmEndpoint.setBeanClassLoader(ClassLoader.getSystemClassLoader());

		//when
		Map<String, String> result = jvmEndpoint.invoke();
		String demandedFlags = result.get(JvmEndpoint.DEMANDED_FLAGS);

		//then
		assertThat(demandedFlags, notNullValue());
		assertThat(demandedFlags, containsString("InitialHeapSize"));
	}

}