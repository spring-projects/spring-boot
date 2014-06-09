package sample.ws;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Created by in329dei on 28-2-14.
 */
@Configuration
@EnableAutoConfiguration
@ComponentScan
public class SampleWsApplication {

	public static void main(String[] args) throws Exception {
		SpringApplication.run(SampleWsApplication.class, args);
	}
}
