/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.actuator;

import java.util.Collections;
import java.util.Map;

import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "another-legacy")
public class SampleLegacyEndpointWithHyphen {

	@ReadOperation
	public Map<String, String> example() {
		return Collections.singletonMap("legacy", "legacy");
	}

}
