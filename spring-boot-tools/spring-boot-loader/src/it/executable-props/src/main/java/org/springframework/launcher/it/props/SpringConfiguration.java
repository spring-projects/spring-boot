/*
 * Copyright 2012-2013 the original author or authors.
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

package org.springframework.boot.load.it.props;

import java.io.IOException;

import javax.annotation.PostConstruct;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.PropertiesLoaderUtils;

/**
 * Spring configuration.
 *
 * @author Phillip Webb
 */
@Configuration
@ComponentScan
public class SpringConfiguration {
	
	private String message = "Jar";
	
	@PostConstruct
	public void init() throws IOException {
		String value = PropertiesLoaderUtils.loadAllProperties("application.properties").getProperty("message");
		if (value!=null) {
			this.message = value; 
		}

	}

	public void run(String... args) {
		System.err.println("Hello Embedded " + this.message + "!");
	}



}
