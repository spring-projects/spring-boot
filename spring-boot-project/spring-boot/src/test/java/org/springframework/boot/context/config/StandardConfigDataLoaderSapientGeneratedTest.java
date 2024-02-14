package org.springframework.boot.context.config;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;

import org.mockito.stubbing.Answer;

import org.springframework.boot.origin.Origin;
import org.springframework.boot.origin.OriginTrackedResource;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.boot.env.PropertySourceLoader;

import org.mockito.MockedStatic;

import java.util.ArrayList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;

import org.junit.jupiter.api.Disabled;

@Timeout(value = 5, threadMode = Timeout.ThreadMode.SEPARATE_THREAD)
class StandardConfigDataLoaderSapientGeneratedTest {

	private final ConfigDataLoaderContext configDataLoaderContextMock = mock(ConfigDataLoaderContext.class);

	private final ConfigDataLocation configDataLocationMock = mock(ConfigDataLocation.class);

	private final ConfigDataLocation configDataLocationMock2 = mock(ConfigDataLocation.class, "load_configDataLocation2");

	private final Origin originMock = mock(Origin.class);

	private final OriginTrackedResource originTrackedResourceMock = mock(OriginTrackedResource.class);

	private final PropertySourceLoader propertySourceLoaderMock = mock(PropertySourceLoader.class);

	private final StandardConfigDataResource resourceMock = mock(StandardConfigDataResource.class, "load_standardConfigDataResource1");

	private final Resource resourceMock2 = mock(Resource.class);

	private final Resource resourceMock3 = mock(Resource.class);

	private final StandardConfigDataReference standardConfigDataReferenceMock = mock(StandardConfigDataReference.class);

	//Sapient generated method id: ${309763d6-5197-33fd-8789-218a32b01068}
	@Test()
	void loadWhenResourceIsEmptyDirectory() throws IOException, ConfigDataNotFoundException {
		/* Branches:
		 * (resource.isEmptyDirectory()) : true
		 */
		//Arrange Statement(s)
		StandardConfigDataResource resourceMock = mock(StandardConfigDataResource.class);
		doReturn(true).when(resourceMock).isEmptyDirectory();
		StandardConfigDataLoader target = new StandardConfigDataLoader();
		//Act Statement(s)
		ConfigData result = target.load(configDataLoaderContextMock, resourceMock);
		ConfigData configData = ConfigData.EMPTY;
		//Assert statement(s)
		//TODO: Please implement equals method in ConfigData for verification to succeed or you need to adjust respective assertion statements
		assertAll("result", () -> {
			assertThat(result, equalTo(configData));
			verify(resourceMock).isEmptyDirectory();
		});
	}
}
