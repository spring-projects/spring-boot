package org.springframework.boot.actuate.info;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

import java.util.Properties;

import org.junit.Test;
import org.springframework.core.env.PropertiesPropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.env.StandardEnvironment;

public class EnvironmentInfoProviderTest {

	@Test
	public void provide_HasTwoRelevantEntries_ShowOnlyRelevantEntries() throws Exception {
		String expectedAppName = "my app name";
		String expectedLanguage = "da-DK";

		Properties properties = new Properties(); 
		properties.setProperty("info.app", expectedAppName);
		properties.setProperty("info.lang", expectedLanguage);
		properties.setProperty("logging.path", "notExpected");
		
		PropertySource<?> propertySource = new PropertiesPropertySource("mysettings", properties);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(propertySource);

		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(environment);
		
		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size(), is(equalTo(2)));
		assertThat((String) actual.get("app"), is(equalTo(expectedAppName)));
        assertThat((String) actual.get("lang"), is(equalTo(expectedLanguage)));
	}
	
	@Test
	public void provide_HasNoRelevantEntries_NoEntries() throws Exception {
		Properties properties = new Properties(); 
		properties.setProperty("logging.path", "notExpected");
		
		PropertySource<?> propertySource = new PropertiesPropertySource("mysettings", properties);

		StandardEnvironment environment = new StandardEnvironment();
		environment.getPropertySources().addLast(propertySource);

		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(environment);
		
		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size(), is(equalTo(0)));
	}
	
	
	@Test
	public void provide_HasNoEntries_NoEntries() throws Exception {
		EnvironmentInfoProvider environmentInfoProvider = new EnvironmentInfoProvider(new StandardEnvironment());
		
		Info actual = environmentInfoProvider.provide();
		assertThat(actual.getDetails().size(), is(equalTo(0)));
	}

}
