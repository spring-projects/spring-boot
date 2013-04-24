package org.springframework.bootstrap.sample.data.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.sample.data.service.CityService;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class SampleController {

	@Autowired
	private CityService cityService;

	@RequestMapping("/")
	@ResponseBody
	public String helloWorld() {
		return cityService.getCity("Bath", "UK").getName();
	}
}
