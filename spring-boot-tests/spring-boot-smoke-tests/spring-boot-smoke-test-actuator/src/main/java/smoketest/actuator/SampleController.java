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

package smoketest.actuator;

import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import jakarta.validation.constraints.NotBlank;

import org.springframework.context.annotation.Description;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

/**
 * SampleController class.
 */
@Controller
@Description("A controller for handling requests for hello messages")
public class SampleController {

	private final HelloWorldService helloWorldService;

	/**
     * Constructs a new SampleController object with the specified HelloWorldService.
     * 
     * @param helloWorldService the HelloWorldService to be used by the SampleController
     */
    public SampleController(HelloWorldService helloWorldService) {
		this.helloWorldService = helloWorldService;
	}

	/**
     * Retrieves a hello message.
     * 
     * @return A map containing a single key-value pair with the message.
     */
    @GetMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, String> hello() {
		return Collections.singletonMap("message", this.helloWorldService.getHelloMessage());
	}

	/**
     * Handles POST requests to the root endpoint ("/") and produces JSON response.
     * 
     * @param message The validated message object received in the request body.
     * @return A map containing the message value, title, and current date.
     */
    @PostMapping(value = "/", produces = MediaType.APPLICATION_JSON_VALUE)
	@ResponseBody
	public Map<String, Object> olleh(@Validated Message message) {
		Map<String, Object> model = new LinkedHashMap<>();
		model.put("message", message.getValue());
		model.put("title", "Hello Home");
		model.put("date", new Date());
		return model;
	}

	/**
     * Handles the request mapping for "/foo" endpoint.
     * 
     * @return the response body as a String
     * @throws IllegalArgumentException if there is a server error
     */
    @RequestMapping("/foo")
	@ResponseBody
	public String foo() {
		throw new IllegalArgumentException("Server error");
	}

	/**
     * Message class.
     */
    protected static class Message {

		@NotBlank(message = "Message value cannot be empty")
		private String value;

		/**
         * Returns the value of the Message object.
         *
         * @return the value of the Message object
         */
        public String getValue() {
			return this.value;
		}

		/**
         * Sets the value of the message.
         * 
         * @param value the new value of the message
         */
        public void setValue(String value) {
			this.value = value;
		}

	}

}
