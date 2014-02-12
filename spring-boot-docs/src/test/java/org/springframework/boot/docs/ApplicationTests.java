package org.springframework.boot.docs;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.SpringApplicationContextLoader;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AutoConfigurationDocsApplication.class, loader=SpringApplicationContextLoader.class)
public class ApplicationTests {

	@Test
	public void contextLoads() {
	}

}
