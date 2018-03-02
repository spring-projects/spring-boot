import java.io.*;
import org.springframework.boot.maven.*

import static org.junit.Assert.assertTrue;

Verify.verifyJar(
	new File( basedir, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar" ), "some.random.Main"
);
File main = new File( basedir, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar")
File backup = new File( basedir, "target/jar-attach-disabled-0.0.1.BUILD-SNAPSHOT.jar.original")
assertTrue 'backup file should exist', backup.exists()


def file = new File(basedir, "build.log")
assertTrue 'main artifact should have been updated',
		file.text.contains("Updating main artifact " + main + " to " + backup)
return file.text.contains ("Installing "+backup)



