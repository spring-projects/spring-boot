package sample.webflux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ExampleController {

	@PostMapping(path = "/", consumes = { MediaType.APPLICATION_JSON_VALUE,
			"!application/xml" }, produces = MediaType.TEXT_PLAIN_VALUE, headers = "X-Custom=Foo", params = "a!=alpha")
	public String example() {
		return "Hello World";
	}

}
