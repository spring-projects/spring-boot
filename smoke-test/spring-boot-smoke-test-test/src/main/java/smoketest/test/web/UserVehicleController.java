/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.test.web;

import smoketest.test.domain.User;
import smoketest.test.service.VehicleDetails;
import smoketest.test.service.VehicleIdentificationNumberNotFoundException;

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

	private final UserVehicleService userVehicleService;

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
