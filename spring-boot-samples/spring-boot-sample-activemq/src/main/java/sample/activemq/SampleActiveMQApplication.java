package sample.activemq;

import org.apache.activemq.command.ActiveMQQueue;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.jms.annotation.EnableJms;

import javax.jms.Queue;

@SpringBootApplication
@EnableJms
public class SampleActiveMQApplication {

	@Bean
	public Queue queue() {
		return new ActiveMQQueue("sample.queue");
	}

	public static void main(String[] args) {
		SpringApplication.run(SampleActiveMQApplication.class, args);
	}

}
