package sample.metrics.atsd.service;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.dao.EmptyResultDataAccessException;
import sample.metrics.atsd.domain.City;
import sample.metrics.atsd.repository.CityRepository;

import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class CityServiceImplTests {
	@Rule
	public ExpectedException thrown = ExpectedException.none();
	private CityServiceImpl cityService;
	private CounterService counterService;
	private CityRepository cityRepository;

	@Before
	public void setUp() throws Exception {
		counterService = mock(CounterService.class);
		cityRepository = mock(CityRepository.class);
		cityService = new CityServiceImpl();
		cityService.setCityRepository(cityRepository);
		cityService.setCounterService(counterService);
	}

	@Test
	public void testGetCity() throws Exception {
		City city = new City();
		when(cityRepository.findById(2L)).thenReturn(city);
		assertSame(city, cityService.getCity(2L));
	}

	@Test
	public void testGetCityNotFound() throws Exception {
		when(cityRepository.findById(3L)).thenThrow(new EmptyResultDataAccessException(1));
		thrown.expect(IllegalArgumentException.class);
		thrown.expectMessage("City not found for id=3");
		cityService.getCity(3L);
	}

	@Test
	public void testCreateCity() throws Exception {
		City city = new City();
		city.setName("Austin");
		city.setCountry("US");
		city.setState("Texas");
		cityService.createCity(city);
		verify(cityRepository).save(city);
		verify(counterService).increment(CityServiceImpl.CITIES_CREATED_METRIC);
	}

	@Test
	public void testDeleteCity() throws Exception {
		cityService.deleteCity(10L);
		verify(cityRepository).remove(10L);
		verify(counterService).increment(CityServiceImpl.CITIES_REMOVED_METRIC);
	}
}