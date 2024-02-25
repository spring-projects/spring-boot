/*
 * Copyright 2012-2024 the original author or authors.
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

package smoketest.mustache;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * WelcomeController class.
 */
@Controller
public class WelcomeController {

	@Value("${application.message:Hello World}")
	private String message = "Hello World";

	/**
     * Retrieves the welcome page.
     * 
     * @param model the model object to be populated with data
     * @return the name of the view to be rendered
     */
    @GetMapping("/")
	public String welcome(Map<String, Object> model) {
		model.put("time", new Date());
		model.put("message", this.message);
		return "welcome";
	}

	/**
     * Handles the request for "/serviceUnavailable" endpoint.
     * 
     * @return A string representation of the response.
     * @throws ServiceUnavailableException if the service is unavailable.
     */
    @RequestMapping("/serviceUnavailable")
	public String ServiceUnavailable() {
		throw new ServiceUnavailableException();
	}

	/**
     * This method is used to handle the "/bang" endpoint.
     * It throws a RuntimeException with the message "Boom".
     * 
     * @return A string representing the result of the operation.
     * @throws RuntimeException if an error occurs during the operation.
     */
    @RequestMapping("/bang")
	public String bang() {
		throw new RuntimeException("Boom");
	}

	/**
     * This method is used to handle the request mapping for "/insufficientStorage" endpoint.
     * It throws an InsufficientStorageException to indicate that there is not enough storage available.
     * 
     * @return A string indicating the error message for insufficient storage.
     * @throws InsufficientStorageException If there is not enough storage available.
     */
    @RequestMapping("/insufficientStorage")
	public String insufficientStorage() {
		throw new InsufficientStorageException();
	}

	/**
     * ServiceUnavailableException class.
     */
    @ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
	private static final class ServiceUnavailableException extends RuntimeException {

	}

	/**
     * InsufficientStorageException class.
     */
    @ResponseStatus(HttpStatus.INSUFFICIENT_STORAGE)
	private static final class InsufficientStorageException extends RuntimeException {

	}

}
