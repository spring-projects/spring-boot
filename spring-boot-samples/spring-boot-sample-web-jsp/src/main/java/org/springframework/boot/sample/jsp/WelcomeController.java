package org.springframework.boot.sample.jsp;

import java.util.Date;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WelcomeController {

	@RequestMapping("/")
	public String welcome(Map<String,Object> model) {
		model.put("time", new Date());
		return "welcome";
	}

}
