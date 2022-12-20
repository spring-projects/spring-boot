package smoketest.jetty.util;

import java.util.Base64;
import java.util.Random;

public class RandomStringUtil {

	private RandomStringUtil() {
	}

	public static String getRandomBase64EncodedString(int length) {
		byte[] responseHeader = new byte[length];
		new Random().nextBytes(responseHeader);
		return Base64.getEncoder().encodeToString(responseHeader);
	}

}
