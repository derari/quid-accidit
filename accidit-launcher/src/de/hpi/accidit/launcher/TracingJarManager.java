package de.hpi.accidit.launcher;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class TracingJarManager {

	private File fTracer;
	private File fModel;
	
	public TracingJarManager() {
	}
	
	public synchronized File getTracerJar() {
		if (fTracer == null || !fTracer.exists()) {
			fTracer = cpFile(fTracer, "/accidit-asm-tracer-1.0-SNAPSHOT-jar-with-dependencies.jar");
			if (fTracer == null) {
				fTracer = cpFile(fTracer, "/lib/accidit-asm-tracer-1.0-SNAPSHOT-jar-with-dependencies.jar");
			}
		}
		return fTracer;
	}
	
	public synchronized File getModelJar() {
		if (fModel == null || !fModel.exists()) {
			fModel = cpFile(fModel, "/accidit-tracer-model-1.0-SNAPSHOT.jar");
			if (fModel == null) {
				fModel = cpFile(fModel, "/lib/accidit-tracer-model-1.0-SNAPSHOT.jar");
			}
		}
		return fModel;
	}
	
	private File cpFile(File target, String name) {
		try {
			if (target == null) {
				int i = name.lastIndexOf('.');
				int d = name.lastIndexOf('/');
				target = File.createTempFile(name.substring(d+1, i+1), name.substring(i));
				target.delete();
				target.deleteOnExit();
			}
			try (InputStream is = getClass().getResourceAsStream(name)) {
				if (is == null) return null;
				Path pTarget = Paths.get(target.toURI());
				Files.copy(is, pTarget);
			}
			return target;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	static private final TracingJarManager INSTANCE = new TracingJarManager();
	
	public static TracingJarManager getInstance() {
		return INSTANCE;
	}
}
