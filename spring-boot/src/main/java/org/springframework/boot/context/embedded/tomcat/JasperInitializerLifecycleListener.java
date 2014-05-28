package org.springframework.boot.context.embedded.tomcat;

import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletException;

import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.core.StandardContext;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * Tomcat {@link LifecycleListener} to initialize Jasper by calling the
 * `JasperInitializer` used in Tomcat 8.
 * 
 * @author Phillip Webb
 */
class JasperInitializerLifecycleListener implements LifecycleListener {

	private static final String JASPER_INITIALIZER_CLASS = "org.apache.jasper.servlet.JasperInitializer";

	private ServletContainerInitializer initializer;

	public JasperInitializerLifecycleListener() {
		this.initializer = getJasperInitializer();
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (this.initializer != null
				&& Lifecycle.CONFIGURE_START_EVENT.equals(event.getType())) {
			onStartup(event);
		}
	}

	private void onStartup(LifecycleEvent event) {
		Assert.isInstanceOf(StandardContext.class, event.getSource());
		StandardContext standardContext = (StandardContext) event.getSource();
		try {
			this.initializer.onStartup(null, standardContext.getServletContext());
		}
		catch (ServletException ex) {
			throw new IllegalStateException(ex);
		}
	}

	private ServletContainerInitializer getJasperInitializer() {
		try {
			Class<?> jasperClass = ClassUtils.forName(JASPER_INITIALIZER_CLASS, null);
			return (ServletContainerInitializer) jasperClass.newInstance();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
