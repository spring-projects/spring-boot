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

package org.springframework.boot.test.context;

import java.util.List;

import org.springframework.test.context.TestExecutionListener;

/**
 * Callback interface trigger from {@link SpringBootTestContextBootstrapper} that can be
 * used to post-process the list of default {@link TestExecutionListener
 * TestExecutionListeners} to be used by a test. Can be used to add or remove existing
 * listeners.
 *
 * @author Phillip Webb
 * @since 1.4.1
 * @see SpringBootTest
 */
@FunctionalInterface
public interface DefaultTestExecutionListenersPostProcessor {

	/**
	 * Post process the list of default {@link TestExecutionListener listeners} to be
	 * used.
	 * @param listeners the source listeners
	 * @return the actual listeners that should be used
	 * @since 3.0.0
	 */
	List<TestExecutionListener> postProcessDefaultTestExecutionListeners(List<TestExecutionListener> listeners);

}
