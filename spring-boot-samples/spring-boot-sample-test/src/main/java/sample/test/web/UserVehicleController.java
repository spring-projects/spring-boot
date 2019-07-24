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

package sample.test.web;

import sample.test.domain.User;
import sample.test.service.VehicleDetails;
import sample.test.service.VehicleIdentificationNumberNotFoundException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller to return vehicle information for a given {@link User}.
 *
 * @author Phillip Webb
 */
@RestController
public class UserVehicleController {

	private UserVehicleService userVehicleService;

	public UserVehicleController(UserVehicleService userVehicleService) {
		this.userVehicleService = userVehicleService;
	}

	@GetMapping(path = "/{username}/vehicle", produces = MediaType.TEXT_PLAIN_VALUE)
	public String getVehicleDetailsText(@PathVariable String username) {
		VehicleDetails details = this.userVehicleService.getVehicleDetails(username);
		return details.getMake() + " " + details.getModel();
	}

	@GetMapping(path = "/{username}/vehicle", produces = MediaType.APPLICATION_JSON_VALUE)
	public VehicleDetails VehicleDetailsJson(@PathVariable String username) {
		return this.userVehicleService.getVehicleDetails(username);
	}

	@GetMapping(path = "/{username}/vehicle.html", produces = MediaType.TEXT_HTML_VALUE)
	public String VehicleDetailsHtml(@PathVariable String username) {
		VehicleDetails details = this.userVehicleService.getVehicleDetails(username);
		String makeAndModel = details.getMake() + " " + details.getModel();
		return "<html><body><h1>" + makeAndModel + "</h1></body></html>";
	}

	@ExceptionHandler
	@ResponseStatus(HttpStatus.NOT_FOUND)
	private void handleVinNotFound(VehicleIdentificationNumberNotFoundException ex) {
	}

}
