/*
 * Copyright 2012-2015 the original author or authors.
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

package sample.metrics.atsd.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import sample.metrics.atsd.domain.City;
import sample.metrics.atsd.service.CityService;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@RestController
public class CityController {
	private CityService cityService;

	@Autowired
	public void setCityService(CityService cityService) {
		this.cityService = cityService;
	}

	@RequestMapping({"/", "/cities"})
	public List<City> list() {
		return this.cityService.findCities();
	}

	@RequestMapping("/cities/add")
	public void add(@ModelAttribute @Validated City city) {
		this.cityService.createCity(city);
	}

	@RequestMapping("/cities/{cityId}/delete")
	public void delete(@PathVariable Long cityId) {
		this.cityService.deleteCity(cityId);
	}

	@RequestMapping("/cities/{cityId}/view")
	public City view(@PathVariable Long cityId) {
		return this.cityService.getCity(cityId);
	}

	@ExceptionHandler(Exception.class)
	@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
	public Object processException(Exception e) {
		return Collections.singletonMap("error", e.toString());
	}

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Object processIllegalArgumentException(Exception e) {
		return Collections.singletonMap("error", e.toString());
	}

	@ExceptionHandler(BindException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Errors processValidationError(BindException ex) {
		List<ValidationError> errors = new ArrayList<ValidationError>();
		BindingResult result = ex.getBindingResult();
		for (FieldError fieldError : result.getFieldErrors()) {
			errors.add(new ValidationError(fieldError.getField(), fieldError.getDefaultMessage()));
		}
		return new Errors(errors);
	}

	public static class Errors {
		private List<ValidationError> errors;

		public Errors(List<ValidationError> errors) {
			this.errors = errors;
		}

		public List<ValidationError> getErrors() {
			return this.errors;
		}
	}

	public static class ValidationError {
		private String field;
		private String message;

		public ValidationError(String field, String message) {
			this.field = field;
			this.message = message;
		}

		public String getField() {
			return this.field;
		}

		public String getMessage() {
			return this.message;
		}
	}
}

