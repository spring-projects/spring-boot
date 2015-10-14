package org.test

import org.springframework.boot.builder.SpringApplicationBuilder
import org.springframework.boot.context.web.SpringBootServletInitializer

import javax.annotation.PostConstruct

@RestController
class WarExample extends SpringBootServletInitializer {

	@PostConstruct
	void onStart(){
		println getClass().getResource('/org/apache/tomcat/InstanceManager.class')
		throw new RuntimeException("onStart error")
	}

	@Override
	protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
		return application.sources(WarExample.class);
	}

}