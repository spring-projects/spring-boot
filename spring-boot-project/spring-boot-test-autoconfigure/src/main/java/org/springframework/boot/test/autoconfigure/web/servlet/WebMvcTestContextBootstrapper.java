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

package org.springframework.boot.test.autoconfigure.web.servlet;

import org.springframework.boot.test.context.SpringBootTestContextBootstrapper;
import org.springframework.test.context.MergedContextConfiguration;
import org.springframework.test.context.TestContextAnnotationUtils;
import org.springframework.test.context.TestContextBootstrapper;
import org.springframework.test.context.web.WebMergedContextConfiguration;

/**
 * {@link TestContextBootstrapper} for {@link WebMvcTest @WebMvcTest} support.
 *
 * @author Phillip Webb
 * @author Artsiom Yudovin
 * @author Lorenzo Dee
 */
class WebMvcTestContextBootstrapper extends SpringBootTestContextBootstrapper {

	/**
     * Processes the merged context configuration by calling the superclass method and creating a new instance of 
     * WebMergedContextConfiguration with the processed configuration and the determined resource base path.
     * 
     * @param mergedConfig the merged context configuration to be processed
     * @return the processed merged context configuration
     */
    @Override
	protected MergedContextConfiguration processMergedContextConfiguration(MergedContextConfiguration mergedConfig) {
		MergedContextConfiguration processedMergedConfiguration = super.processMergedContextConfiguration(mergedConfig);
		return new WebMergedContextConfiguration(processedMergedConfiguration, determineResourceBasePath(mergedConfig));
	}

	/**
     * Retrieves the properties specified in the {@link WebMvcTest} annotation for the given test class.
     * 
     * @param testClass the test class to retrieve the properties from
     * @return an array of properties specified in the {@link WebMvcTest} annotation, or null if the annotation is not present
     */
    @Override
	protected String[] getProperties(Class<?> testClass) {
		WebMvcTest webMvcTest = TestContextAnnotationUtils.findMergedAnnotation(testClass, WebMvcTest.class);
		return (webMvcTest != null) ? webMvcTest.properties() : null;
	}

}
