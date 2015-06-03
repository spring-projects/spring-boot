package sample.rs.client;


import javax.ws.rs.core.Response;

import org.apache.cxf.helpers.IOUtils;

import java.io.InputStream;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.SpringApplicationConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.IntegrationTest;

import com.sun.jersey.api.client.Client;
import com.sun.jersey.api.client.ClientResponse;
import com.sun.jersey.api.client.WebResource;

import sample.rs.service.SampleRestWSApplication;

import org.springframework.test.annotation.DirtiesContext;

import org.springframework.test.context.web.WebAppConfiguration;
import javax.ws.rs.core.MediaType;


import static org.junit.Assert.assertEquals;

/**
 * Basic integration tests for demo application.
 *
 * @author Dave Syer
 */
@RunWith(SpringJUnit4ClassRunner.class)
@SpringApplicationConfiguration(classes = { SampleRestWSApplication.class })
@WebAppConfiguration
@IntegrationTest("server.port:0")
@DirtiesContext
public class SampleRestWSApplicationTests {

	@Value("${local.server.port}")
	private int port;

	@Test
	public void rsTextClient() throws Exception {
		String value = null;
		try{
			String name = "Elan";
		 	Client client = Client.create();
		 	WebResource webResource = client.resource("http://localhost:"+this.port+"/services/helloservice/sayHello/txt/"+name);
	 		ClientResponse response = webResource.accept("text/plain").get(ClientResponse.class);
			 value = response.getEntity(String.class);
			System.out.println(value);
		}catch(Exception e) {
			e.printStackTrace();
		}
		assertEquals(value, "Hello Elan, Welcome to CXF RS Spring Boot World!!!");
	}


	@Test
	public void rsJsonClient() throws Exception {
		String value = null;
		try{
			String name = "Elan";
		 	Client client = Client.create();
		 	WebResource webResource = client.resource("http://localhost:"+this.port+"/services/helloservice/sayHello/json/"+name);
	 		ClientResponse response = webResource.accept(MediaType.APPLICATION_JSON).get(ClientResponse.class);
			 value = response.getEntity(String.class);
			System.out.println(value);
		}catch(Exception e) {
			e.printStackTrace();
		}
		assertEquals(value, "{hello='Elan, Welcome to CXF RS Spring Boot World!!!'}");
	}

}
