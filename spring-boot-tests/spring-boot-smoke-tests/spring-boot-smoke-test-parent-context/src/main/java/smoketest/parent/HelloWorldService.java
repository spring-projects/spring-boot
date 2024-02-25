/*
 * Copyright 2012-2019 the original author or authors.
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

package smoketest.parent;

import org.springframework.stereotype.Component;

/**
 * HelloWorldService class.
 */
@Component
public class HelloWorldService {

	private final ServiceProperties configuration;

	/**
     * Constructs a new HelloWorldService with the specified configuration.
     * 
     * @param configuration the service properties configuration
     */
    public HelloWorldService(ServiceProperties configuration) {
		this.configuration = configuration;
	}

	/**
     * Returns a hello message for the given name.
     * 
     * @param name the name to include in the hello message
     * @return the hello message
     */
    public String getHelloMessage(String name) {
		return this.configuration.getGreeting() + " " + name;
	}

}
