/*
 * Copyright 2012-2014 the original author or authors.
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
 *
 *
 * Examples:
 *
 * Get all users:
 * curl -i http://localhost:8080/users
 *
 * Create a user:
 * curl -i -H "Content-Type:application/json" -d '{"lastName": "Simpson", "firstName":"Homer"}' http://localhost:8080/users/user
 *
 * Get a specific user:
 * curl -i http://localhost:8080/users/user/2
 *
 * @author Monica Granbois
 */
package sample.data.jdbc.web;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import sample.data.jdbc.domain.User;
import sample.data.jdbc.service.UserService;
import javax.validation.Valid;
import java.util.List;


@RestController
@RequestMapping ("users")
public class UserController {

	private UserService userService;

	@Autowired
	public UserController(UserService userService) {
		this.userService = userService;
	}

	@RequestMapping (value = "user/{id}", method = RequestMethod.GET)
	public User getUser(
			@PathVariable ("id")
			int id) {
		return userService.getUser(id);
	}

	@RequestMapping (value = "user", method = RequestMethod.POST)
	@ResponseStatus (HttpStatus.CREATED)
	public User createUser(
			@RequestBody
			@Valid
			User user) {
		return userService.createUser(user);
	}


	@RequestMapping (method = RequestMethod.GET)
	public List<User> getUsers() {
		return userService.getUsers();
	}


	@ExceptionHandler
	@ResponseStatus (value = HttpStatus.NOT_FOUND, reason = "user does not exist")
	public void handleUserNotFoundException(EmptyResultDataAccessException ex) {
		ex.printStackTrace(System.err);
	}

	@ExceptionHandler
	@ResponseStatus (value = HttpStatus.BAD_REQUEST, reason = "invalid request parameters")
	public void handleBadArgumentException(MethodArgumentNotValidException ex) {
		ex.printStackTrace(System.err);
	}

	@ExceptionHandler
	@ResponseStatus (value = HttpStatus.CONFLICT, reason = "user already exists")
	public void handleDuplicateUser(DuplicateKeyException ex) {
		ex.printStackTrace(System.err);
	}

	@ExceptionHandler
	@ResponseStatus (value = HttpStatus.INTERNAL_SERVER_ERROR, reason = "an internal server error occurred")
	public void handleException(Exception ex) {
		ex.printStackTrace(System.err);
	}


}
