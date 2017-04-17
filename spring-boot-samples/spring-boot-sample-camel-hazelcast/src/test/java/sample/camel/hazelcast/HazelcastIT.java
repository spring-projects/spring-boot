
package sample.camel.hazelcast;

import org.apache.camel.EndpointInject;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.test.junit4.CamelTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import static org.junit.Assert.fail;

@RunWith(SpringRunner.class)
@SpringBootTest
public class HazelcastIT {

	private final static String HAZELCAST_TEST_QUEUE_NAME = "hazelcast:seda:hazelcastTest";

	@EndpointInject
	private ProducerTemplate template;

	@Test
	public void contextLoads() {
		try {
			template.sendBody(HAZELCAST_TEST_QUEUE_NAME, "");
		} catch (Exception e) {
			fail("Can not put in the route.");
		}
	}
}
