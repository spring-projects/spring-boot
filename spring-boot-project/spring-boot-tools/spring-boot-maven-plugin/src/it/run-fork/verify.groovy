import java.lang.reflect.Method;

import static org.junit.Assert.assertTrue

def boolean isJava13OrLater() {
	for (Method method : String.class.getMethods()) {
		if (method.getName().equals("stripIndent")) {
			return true;
		}
	}
	return false;
}

def file = new File(basedir, "build.log")
assertTrue file.text.contains("I haz been run from '$basedir'")
if (isJava13OrLater()) {
	assertTrue file.text.contains("JVM argument(s): -XX:TieredStopAtLevel=1")
}
else {
	assertTrue file.text.contains("JVM argument(s): -Xverify:none -XX:TieredStopAtLevel=1")
}
