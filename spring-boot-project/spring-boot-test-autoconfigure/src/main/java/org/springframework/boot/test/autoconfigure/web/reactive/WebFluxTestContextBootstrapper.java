/*
 * Copyright 2012-2023 the original author or authors.
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

package org.springframework.boot.test.autoconfigure.web.reactive;

import org.springframework.boot.test.context.ReactiveWebMergedContextConfiguration;
import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;

/**
 * {@link TestContextBootstrapper} for {@link WebFluxTest @WebFluxTest} support.
 *
 * @author Stephane Nicoll
 * @author Artsiom Yudovin
 */
class WebFluxTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	/**
     * Processes the merged context configuration by creating a new ReactiveWebMergedContextConfiguration
     * based on the super class's processed merged context configuration.
     * 
     * @param mergedConfig the merged context configuration to be processed
     * @return the processed merged context configuration
     */
    @Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		return new ReactiveWebMergedContextConfiguration(super.processMergedContextConfiguration(mergedConfig));
	}

	/**
     * Retrieves the properties specified in the {@link WebFluxTest} annotation for the given test class.
     * 
     * @param testClass the test class to retrieve the properties for
     * @return an array of properties specified in the {@link WebFluxTest} annotation, or null if the annotation is not present
     */
    @Override
	protected String[] getProperties(Class<?> testClass) {
		WebFluxTest webFluxTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, WebFluxTest.class);
		return (webFluxTest != null) ? webFluxTest.properties() : null;
	}

}
