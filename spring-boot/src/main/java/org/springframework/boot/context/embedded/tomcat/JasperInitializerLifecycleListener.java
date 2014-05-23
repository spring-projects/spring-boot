package org.springframework.boot.context.embedded.tomcat;

import org.apache.catalina.LifecycleEvent;
import org.apache.catalina.LifecycleListener;
import org.springframework.boot.context.embedded.ServletContextInitializer;
import org.springframework.util.ClassUtils;

/**
 * Tomcat {@link LifecycleListener} to initialize Jasper by calling the
 * `JasperInitializer` used in Tomcat 8.
 * 
 * @author Phillip Webb
 */
class JasperInitializerLifecycleListener implements LifecycleListener {

	private static final String JASPER_INITIALIZER_CLASS = "org.apache.jasper.servlet.JasperInitializer";

	private final ServletContextInitializerLifecycleListener delegate;

	public JasperInitializerLifecycleListener() {
		ServletContextInitializer initializer = getJasperInitializer();
		if (initializer != null) {
			this.delegate = new ServletContextInitializerLifecycleListener(initializer);
		}
		else {
			this.delegate = null;
		}
	}

	@Override
	public void lifecycleEvent(LifecycleEvent event) {
		if (this.delegate != null) {
			this.delegate.lifecycleEvent(event);
		}
	}

	private ServletContextInitializer getJasperInitializer() {
		try {
			Class<?> jasperClass = ClassUtils.forName(JASPER_INITIALIZER_CLASS, null);
			return (ServletContextInitializer) jasperClass.newInstance();
		}
		catch (Exception ex) {
			return null;
		}
	}

}
