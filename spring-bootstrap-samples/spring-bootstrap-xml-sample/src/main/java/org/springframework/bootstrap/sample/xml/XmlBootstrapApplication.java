package org.springframework.bootstrap.sample.xml;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.bootstrap.CommandLineRunner;
import org.springframework.bootstrap.SpringApplication;
import org.springframework.bootstrap.sample.xml.service.HelloWorldService;

public class XmlBootstrapApplication implements CommandLineRunner {

	@Autowired
	private HelloWorldService helloWorldService;

	@Override
	public void run(String... args) {
		System.out.println(this.helloWorldService.getHelloMessage());
	}

	public static void main(String[] args) throws Exception {
		// TODO: to make this a pure XML example, will need <bootstrap:auto-configure/>
		SpringApplication.run("classpath:/META-INF/application-context.xml", args);
	}
}
