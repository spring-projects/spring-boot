package sample.oauth2.client;

import java.security.Principal;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Madhura Bhave
 */
@RestController
public class ExampleController {

	@RequestMapping("/")
	public String email(Principal principal) {
		return "Hello " + principal.getName();
	}

}
