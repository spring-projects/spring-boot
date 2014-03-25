package org.springframework.boot.util;

import org.junit.Test;

import static org.junit.Assert.assertNotNull;

/**
 * Tests for {@link org.springframework.boot.util.SystemUtils}.
 *
 * @author Jakub Kubrynski
 */
public class SystemUtilsTest {

	@Test
	public void shouldGetApplicationPid() throws Exception {
		//when
		String applicationPid = SystemUtils.getApplicationPid();

		//then
		assertNotNull(applicationPid);
	}
}
