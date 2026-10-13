/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.data.jpa.web;

import smoketest.data.jpa.service.CityService;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

	@Autowired
	private CityService cityService;

	@GetMapping("/")
	@ResponseBody
	@Transactional(readOnly = true)
	public String helloWorld() {
		return this.cityService.getCity("Bath", "UK").getName();
	}

}
