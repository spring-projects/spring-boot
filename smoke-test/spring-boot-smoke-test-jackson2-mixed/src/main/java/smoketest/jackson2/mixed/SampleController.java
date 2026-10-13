/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SampleController {

	@GetMapping("/names")
	List<Name> names() {
		return List.of(new Name("Spring", "Boot"));
	}

}
