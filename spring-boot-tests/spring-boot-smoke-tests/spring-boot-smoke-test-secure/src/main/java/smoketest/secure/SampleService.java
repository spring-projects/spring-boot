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

package smoketest.secure;

import org.springframework.security.access.annotation.Secured;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

/**
 * SampleService class.
 */
@Service
public class SampleService {

	/**
     * This method is secured and can only be accessed by users with the "ROLE_USER" role.
     * It returns a string "Hello Security".
     *
     * @return the string "Hello Security"
     */
    @Secured("ROLE_USER")
	public String secure() {
		return "Hello Security";
	}

	/**
     * This method is authorized to be accessed by any user.
     * 
     * @return The string "Hello World"
     */
    @PreAuthorize("true")
	public String authorized() {
		return "Hello World";
	}

	/**
     * This method is used to deny access and return a goodbye message.
     * 
     * @return A string representing the goodbye message.
     */
    @PreAuthorize("false")
	public String denied() {
		return "Goodbye World";
	}

}
