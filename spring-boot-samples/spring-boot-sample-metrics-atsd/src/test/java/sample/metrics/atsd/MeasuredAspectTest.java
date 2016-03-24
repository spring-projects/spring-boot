package sample.metrics.atsd;

import org.junit.Before;
import org.junit.Test;
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory;
import org.springframework.boot.actuate.metrics.CounterService;
import org.springframework.boot.actuate.metrics.GaugeService;

import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;

public class MeasuredAspectTest {
	private MeasuredAspect measuredAspect;
	private CounterService counterService;
	private GaugeService gaugeService;

	@Before
	public void setUp() throws Exception {
		counterService = mock(CounterService.class);
		gaugeService = mock(GaugeService.class);
		measuredAspect = new MeasuredAspect();
		measuredAspect.setCounterService(counterService);
		measuredAspect.setGaugeService(gaugeService);
	}

	@Test
	public void testDoProfilingMethodMarked() throws Exception {
		AspectJProxyFactory factory = new AspectJProxyFactory(new ParticularMethodAnnotated());
		factory.addAspect(measuredAspect);
		TwoMethods proxy = factory.getProxy();
		proxy.methodOne();
		verify(counterService, never()).increment(anyString());
		verify(gaugeService, never()).submit(anyString(), anyDouble());

		proxy.methodTwo();
		String expectedMetric = "method.sample.metrics.atsd.ParticularMethodAnnotated.methodTwo";
		verify(counterService).increment(expectedMetric);
		verify(gaugeService).submit(eq(expectedMetric), anyDouble());
	}

	@Test
	public void testDoProfilingClassMarked() throws Exception {
		AspectJProxyFactory factory = new AspectJProxyFactory(new WholeClassAnnotated());
		factory.addAspect(measuredAspect);
		TwoMethods proxy = factory.getProxy();
		proxy.methodOne();
		String expectedMetric = "method.sample.metrics.atsd.WholeClassAnnotated.methodOne";
		verify(counterService).increment(expectedMetric);
		verify(gaugeService).submit(eq(expectedMetric), anyDouble());

		reset(counterService, gaugeService);

		try {
			proxy.methodTwo();
		} catch (UnsupportedOperationException e) {
			// expected
		}
		expectedMetric = "method.sample.metrics.atsd.WholeClassAnnotated.methodTwo.failed";
		verify(counterService).increment(expectedMetric);
		verify(gaugeService).submit(eq(expectedMetric), anyDouble());
		verify(counterService).increment(expectedMetric);
		verify(gaugeService).submit(eq(expectedMetric), anyDouble());
	}
}