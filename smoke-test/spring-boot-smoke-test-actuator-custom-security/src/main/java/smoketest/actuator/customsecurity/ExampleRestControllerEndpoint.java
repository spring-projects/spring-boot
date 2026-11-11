/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator.customsecurity;

import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Component
@RestControllerEndpoint(id = "example")
@SuppressWarnings("removal")
public class ExampleRestControllerEndpoint {

	@GetMapping("/echo")
	public ResponseEntity<String> echo(@RequestParam("text") String text) {
		return ResponseEntity.ok().header("echo", text).body(text);
	}

}
