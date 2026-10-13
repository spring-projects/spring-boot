/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.devtools;

import jakarta.annotation.PostConstruct;

import org.springframework.stereotype.Controller;

@Controller
public class MyController {

	@PostConstruct
	public void slowRestart() throws InterruptedException {
		Thread.sleep(5000);
	}

}
