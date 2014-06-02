package org.springframework.boot.autoconfigure.security.jpa;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

/**
 * The EntityScanRegistrar can cause problems with Spring security and its eager
 * instantiation needs. This test is designed to fail if the Entities can't be scanned
 * because the registrar doesn't get a callback with the right beans (essentially because
 * their instantiation order was accelerated by Security).
 * 
 * @author Dave Syer
 * 
 * @since 1.1
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = Main.class)
public class JpaUserDetailsTests {

	@Test
	public void contextLoads() throws Exception {
	}

	public static void main(String[] args) throws Exception {
		SpringApplication.run(Main.class, args);
	}

}