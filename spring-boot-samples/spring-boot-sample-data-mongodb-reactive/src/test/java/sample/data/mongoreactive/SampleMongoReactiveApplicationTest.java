package sample.data.mongoreactive;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.ClassRule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.rule.OutputCapture;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Tests for {@link SampleMongoReactiveApplicationTest}.
 *
 * @author Raja Kolli
 */
@RunWith(SpringRunner.class)
@SpringBootTest
public class SampleMongoReactiveApplicationTest {

	@ClassRule
	public static OutputCapture outputCapture = new OutputCapture();

	@Test
	public void testDefaultSettings() throws Exception {
		String output = SampleMongoReactiveApplicationTest.outputCapture.toString();
		assertThat(output).contains("firstName='Alice', lastName='Smith'");
	}

}
