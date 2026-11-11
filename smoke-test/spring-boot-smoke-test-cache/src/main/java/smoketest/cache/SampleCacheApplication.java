/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.cache;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableCaching
@EnableScheduling
@SpringBootApplication
public class SampleCacheApplication {

	public static void main(String[] args) {
		new SpringApplicationBuilder().sources(SampleCacheApplication.class).profiles("app").run(args);
	}

}
