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

package org.test;

import org.test.SampleApplication.SampleApplicationRuntimeHints;

import org.springframework.aop.framework.AopProxyUtils;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.aot.hint.RuntimeHintsRegistrar;
import org.springframework.boot.SpringApplication;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.ImportRuntimeHints;
import org.springframework.stereotype.Service;

/**
 * SampleApplication class.
 */
@Configuration(proxyBeanMethods = false)
@ImportRuntimeHints(SampleApplicationRuntimeHints.class)
public class SampleApplication {

	/**
     * The main method is the entry point of the application.
     * It starts the Spring Boot application by calling the SpringApplication.run() method.
     * 
     * @param args the command line arguments passed to the application
     */
    public static void main(String[] args) {
		SpringApplication.run(SampleApplication.class, args);
	}

	/**
     * SampleApplicationRuntimeHints class.
     */
    static class SampleApplicationRuntimeHints implements RuntimeHintsRegistrar {

		/**
         * Registers hints for runtime behavior.
         * 
         * @param hints the runtime hints to register
         * @param classLoader the class loader to use for loading classes
         */
        @Override
		public void registerHints(RuntimeHints hints, ClassLoader classLoader) {
			// Force creation of at least one JDK proxy
			hints.proxies().registerJdkProxy(AopProxyUtils.completeJdkProxyInterfaces(Service.class));
		}
	}

}
