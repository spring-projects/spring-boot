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

package smoketest.tomcat.jsp;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;
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
     * This method is used to handle the "/fail" endpoint.
     * It throws a custom exception called MyException with the message "Oh dear!".
     * 
     * @return A string representing the error message.
     * @throws MyException If an error occurs during the execution of the method.
     */
    @RequestMapping("/fail")
	public String fail() {
		throw new MyException("Oh dear!");
	}

	/**
     * This method is mapped to the "/fail2" endpoint and throws an IllegalStateException.
     * 
     * @return A string representing the error message.
     * @throws IllegalStateException if an illegal state is encountered.
     */
    @RequestMapping("/fail2")
	public String fail2() {
		throw new IllegalStateException();
	}

	/**
     * Handles MyException and returns a MyRestResponse with a custom message.
     * 
     * @param exception the MyException to be handled
     * @return a MyRestResponse containing the custom message to be sent back to the client
     */
    @ExceptionHandler(MyException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public @ResponseBody MyRestResponse handleMyRuntimeException(MyException exception) {
		return new MyRestResponse("Some data I want to send back to the client.");
	}

}
