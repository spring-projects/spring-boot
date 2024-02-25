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

package smoketest.jetty.jsp;

/**
 * MyRestResponse class.
 */
public class MyRestResponse {

	private final String message;

	/**
	 * Constructs a new MyRestResponse object with the specified message.
	 * @param message the message to be set for the MyRestResponse object
	 */
	public MyRestResponse(String message) {
		this.message = message;
	}

	/**
	 * Returns the message of the MyRestResponse object.
	 * @return the message of the MyRestResponse object
	 */
	public String getMessage() {
		return this.message;
	}

}
