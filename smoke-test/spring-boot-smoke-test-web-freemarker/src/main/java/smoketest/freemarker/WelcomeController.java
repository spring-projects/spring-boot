/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.freemarker;

import java.util.Date;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class WelcomeController {

	@Value("${application.message:Hello World}")
	private String message = "Hello World";

	@GetMapping("/")
	public String welcome(Map<String, Object> model) {
		model.put("time", new Date());
		model.put("message", this.message);
		return "welcome";
	}

	@GetMapping("insufficient-storage")
	void outOfStorage() {
		throw new ResponseStatusException(HttpStatus.INSUFFICIENT_STORAGE);
	}

}
