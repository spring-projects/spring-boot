/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.grpcserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class SampleGrpcServerApplication {

	public static void main(String[] args) {
		SpringApplication.run(SampleGrpcServerApplication.class, args);
	}

}
