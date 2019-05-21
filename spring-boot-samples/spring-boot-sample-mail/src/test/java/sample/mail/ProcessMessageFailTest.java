package sample.mail;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

import static org.assertj.core.api.Assertions.assertThat;

@RunWith(SpringRunner.class)
@SpringBootTest
@TestPropertySource(locations = { "classpath:application.properties", "classpath:fail.properties" })
public class ProcessMessageFailTest {

    private static ByteArrayOutputStream outContent;

    @BeforeClass
    public static void setup(){
        outContent = new ByteArrayOutputStream();
        System.setErr(new PrintStream(outContent));
    }

    @Ignore
    @Test
    public void processMessagePrintsExceptionWhenInvalidAddress() {
        assertThat(outContent.toString()).contains("javax.mail.SendFailedException: Invalid Addresses;");
    }
}
