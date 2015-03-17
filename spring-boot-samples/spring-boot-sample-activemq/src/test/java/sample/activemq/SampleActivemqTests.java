package sample.activemq;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.OutputCapture;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import javax.jms.JMSException;

import static org.junit.Assert.assertTrue;

@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = {SampleActiveMQApplication.class})
public class SampleActiveMQTests {

	@Rule
	public OutputCapture outputCapture = new OutputCapture();

	@Autowired
	private Producer producer;

	@Test
	public void test_queue() throws InterruptedException, JMSException {
		System.out.println("Starting queue");
		producer.sendToQueue();
		Thread.sleep(1000L);
		assertTrue(outputCapture.toString().contains("Hi Queue!!!"));
	}

}
