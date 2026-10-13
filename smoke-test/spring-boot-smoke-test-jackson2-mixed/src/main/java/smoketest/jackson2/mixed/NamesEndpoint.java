/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.jackson2.mixed;

import java.util.List;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

/**
 * Names endpoint.
 *
 * @author Andy Wilkinson
 */
@Component
@Endpoint(id = "names")
public class NamesEndpoint {

	@ReadOperation
	List<Name> names() {
		return List.of(new Name("Spring", "Boot"));
	}

}
