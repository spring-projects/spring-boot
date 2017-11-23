package org.springframework.boot.devtools.filewatch;

import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.time.Duration;
import java.util.Date;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.boot.devtools.filewatch.ChangedFile.Type;
import org.springframework.boot.devtools.remote.client.ClassPathChangeUploader;
import org.springframework.util.FileCopyUtils;

public class FileSystemNIOWatcherTest {

	private static final Log logger = LogFactory.getLog(ClassPathChangeUploader.class);

	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void testStartStopThread() throws Exception {
		CountDownLatch lock = new CountDownLatch(1);
		FileSystemNIOWatcher watcher = new FileSystemNIOWatcher(Duration.ofMillis(10),
				Duration.ofMillis(0));
		watcher.addSourceFolder(temporaryFolder.getRoot());
		assertTrue(!watcher.isAlive());
		watcher.start();
		lock.await(10, TimeUnit.MILLISECONDS);
		assertTrue(watcher.isAlive());
		watcher.stop();
		lock.await(10, TimeUnit.MILLISECONDS);
		assertTrue(!watcher.isAlive());
	}

	@Test
	public void testFileAdded() throws Exception {
		CountDownLatch lock = new CountDownLatch(1);
		final AtomicBoolean notifiedDeleted = new AtomicBoolean(false);
		final AtomicBoolean notifiedAdded = new AtomicBoolean(false);
		final AtomicBoolean notifiedModified = new AtomicBoolean(false);
		FileSystemNIOWatcher watcher = new FileSystemNIOWatcher(Duration.ofMillis(10),
				Duration.ofMillis(0));
		watcher.addSourceFolder(temporaryFolder.getRoot());
		watcher.addListener(new FileChangeListener() {
			@Override
			public void onChange(Set<ChangedFiles> changeSet) {
				for (ChangedFiles set : changeSet) {
					Set<ChangedFile> files = set.getFiles();
					for (ChangedFile f : files) {

						if (notifiedModified.get()) {
							File file = f.getFile();
							Type type = f.getType();
							logger.debug("File: " + file + ", type: " + type);
							assertTrue(!file.exists());
							assertTrue(type.equals(Type.DELETE));
							notifiedDeleted.set(true);
						}
						else if (notifiedAdded.get()) {
							File file = f.getFile();
							Type type = f.getType();
							logger.debug("File: " + file + ", type: " + type);
							assertTrue(type.equals(Type.MODIFY));
							notifiedModified.set(true);
							file.delete();
						}
						else {
							File file = f.getFile();
							Type type = f.getType();
							logger.debug("File: " + file + ", type: " + type);
							assertTrue(file.exists());
							assertTrue(type.equals(Type.ADD));
							notifiedAdded.set(true);
						}
					}
				}
			}
		});
		watcher.start();
		lock.await(10, TimeUnit.MILLISECONDS);
		File file = createNewFile("abc", new Date().getTime());
		assertTrue(file.exists());
		lock.await(1000, TimeUnit.MILLISECONDS);
		assertTrue(notifiedModified.get());
		watcher.stop();
	}

	private File createNewFile(String content, long lastModified) throws IOException {
		File file = this.temporaryFolder.newFile();
		setupFile(file, content, lastModified);
		return file;
	}

	private void setupFile(File file, String content, long lastModified)
			throws IOException {
		FileCopyUtils.copy(content.getBytes(), file);
		file.setLastModified(lastModified);
		logger.debug("New tmp file  : " + file.exists() + ", " + file.getAbsolutePath());
	}

}
