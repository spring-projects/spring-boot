/*
 * Copyright 2012-present the original author or authors.
 */

package smoketest.rabbitmq.multiple;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.rabbitmq.RabbitMQContainer;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbitmq.client.RabbitAmqpTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.health.actuate.endpoint.CompositeHealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthDescriptor;
import org.springframework.boot.health.actuate.endpoint.HealthEndpoint;
import org.springframework.boot.health.actuate.endpoint.IndicatedHealthDescriptor;
import org.springframework.boot.health.contributor.Status;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testsupport.container.TestImage;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Smoke tests for an RabbitMQ using both the AMQP 0.9 and AMQP 1.0 clients.
 */
@SpringBootTest(properties = "management.endpoint.health.show-details=always")
@Testcontainers(disabledWithoutDocker = true)
class SampleRabbitMultipleApplicationTests {

	@Container
	static final RabbitMQContainer rabbit = TestImage.forContainer(RabbitMQContainer.class);

	@DynamicPropertySource
	static void brokerProperties(DynamicPropertyRegistry registry) {
		List.of("spring.rabbitmq", "spring.amqp.rabbitmq").forEach((prefix) -> {
			registry.add(prefix + ".host", rabbit::getHost);
			registry.add(prefix + ".port", rabbit::getAmqpPort);
			registry.add(prefix + ".username", rabbit::getAdminUsername);
			registry.add(prefix + ".password", rabbit::getAdminPassword);
		});
	}

	@Autowired
	private HealthEndpoint healthEndpoint;

	@Autowired
	private RabbitTemplate rabbitTemplate;

	@Autowired
	private RabbitAmqpTemplate rabbitAmqpTemplate;

	@Test
	void bothHealthIndicatorsAreRegisteredAndUp() {
		HealthDescriptor health = this.healthEndpoint.health();
		assertThat(health).isInstanceOfSatisfying(CompositeHealthDescriptor.class, (composite) -> {
			assertThat(composite.getStatus()).isEqualTo(Status.UP);
			Map<String, HealthDescriptor> components = composite.getComponents();
			assertThat(components).isNotNull();
			assertThat(components).containsKeys("rabbit", "amqpRabbit");
			assertThat(components.get("rabbit")).isInstanceOfSatisfying(IndicatedHealthDescriptor.class, (rabbit) -> {
				assertThat(rabbit.getStatus()).isEqualTo(Status.UP);
				assertThat(rabbit.getDetails()).containsKey("version");
			});
			assertThat(components.get("amqpRabbit")).isInstanceOfSatisfying(IndicatedHealthDescriptor.class,
					(amqpRabbit) -> {
						assertThat(amqpRabbit.getStatus()).isEqualTo(Status.UP);
						assertThat(amqpRabbit.getDetails()).containsKey("version");
					});
		});
	}

	@Test
	void messageSentWithAmqp09CanBeReceivedWithAmqp10() throws Exception {
		this.rabbitTemplate.convertAndSend("foo", "from-amqp-0.9");
		assertThat(this.rabbitAmqpTemplate.receiveAndConvert("foo").get(30, TimeUnit.SECONDS))
			.isEqualTo("from-amqp-0.9");
	}

	@Test
	void messageSentWithAmqp10CanBeReceivedWithAmqp09() throws Exception {
		this.rabbitAmqpTemplate.convertAndSend("foo", "from-amqp-1.0").get(30, TimeUnit.SECONDS);
		assertThat(this.rabbitTemplate.receiveAndConvert("foo", 30_000)).isEqualTo("from-amqp-1.0");
	}

}
