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

package org.springframework.boot.docs.features.springapplication.fluentbuilderapi;

import org.springframework.boot.Banner;
import org.springframework.boot.builder.SpringApplicationBuilder;

/**
 * MyApplication class.
 */
public class MyApplication {

	/**
     * This method creates a hierarchy with a disabled banner.
     * It uses the SpringApplicationBuilder to configure the parent and child classes,
     * sets the banner mode to OFF, and runs the application with the provided arguments.
     * 
     * @param args the command line arguments
     */
    public void hierarchyWithDisabledBanner(String[] args) {
		// tag::code[]
		new SpringApplicationBuilder().sources(Parent.class)
			.child(Application.class)
			.bannerMode(Banner.Mode.OFF)
			.run(args);
		// end::code[]
	}

	/**
     * Parent class.
     */
    static class Parent {

	}

	/**
     * Application class.
     */
    static class Application {

	}

}
