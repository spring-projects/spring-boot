/*
 * Copyright 2012-2021 the original author or authors.
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

package org.springframework.boot.docs.howto.traditionaldeployment.convertexistingapplication.both;

import org.springframework.boot.Banner;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;

/**
 * MyApplication class.
 */
@SpringBootApplication
public class MyApplication extends SpringBootServletInitializer {

	/**
	 * Configures the SpringApplicationBuilder for the MyApplication class.
	 * @param builder the SpringApplicationBuilder to be configured
	 * @return the configured SpringApplicationBuilder
	 */
	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder builder) {
		return customizerBuilder(builder);
	}

	/**
	 * The main method of the MyApplication class.
	 * @param args the command line arguments
	 */
	public static void main(String[] args) {
		customizerBuilder(new SpringApplicationBuilder()).run(args);
	}

	/**
	 * This method is a customizer for the SpringApplicationBuilder. It takes a
	 * SpringApplicationBuilder object as a parameter and returns a modified builder. The
	 * modified builder includes the sources of the MyApplication class and sets the
	 * banner mode to OFF.
	 * @param builder the SpringApplicationBuilder object to be customized
	 * @return the modified SpringApplicationBuilder object
	 */
	private static SpringApplicationBuilder customizerBuilder(SpringApplicationBuilder builder) {
		return builder.sources(MyApplication.class).bannerMode(Banner.Mode.OFF);
	}

}
