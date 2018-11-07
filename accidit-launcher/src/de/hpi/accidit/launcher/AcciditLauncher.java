package de.hpi.accidit.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.jdt.core.IJavaProject;

public class AcciditLauncher {
	
	private File traceDir;
	private File tmpFile;
	
	public AcciditLauncher(File workingDir) {
		traceDir = new File(workingDir, "accidit_trace");
		try {
			tmpFile = File.createTempFile("accidit_canary", ".log");
			tmpFile.delete();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
	
	public void collectExecutionArguments(String entryPoint,
			ILaunchConfiguration configuration, List<String> vmArguments,
			List<String> programArguments) throws CoreException {
		if (!vmArguments.stream().anyMatch(s -> s.startsWith("-Xmx"))) {
			vmArguments.add("-Xmx1G");
		}
		vmArguments.add("-noverify");
				
		vmArguments.add(getAgentArg(entryPoint));
	}
	
	public String getVMArguments(String entryPoint, ILaunchConfiguration configuration,
			String vmArgs) {
		StringBuilder sb = new StringBuilder(vmArgs);
		if (!vmArgs.contains(" -Xmx")) sb.append(" -Xmx1G");
		sb.append(" -noverify");
		sb.append(" \"").append(getAgentArg(entryPoint)).append("\"");
		return sb.toString();
	}
	
	protected String getAgentArg(String entryPoint) {
		String arg = "-javaagent:"
				+ TracingJarManager.getInstance().getTracerJar().getAbsolutePath()
				+ "="
				+ TracingJarManager.getInstance().getModelJar().getAbsolutePath()
				+ "#" + entryPoint + "#"
				+ traceDir.getAbsolutePath()
				+ "#n#"
				+ tmpFile.getAbsolutePath();
		System.out.println(arg);
		return arg;
	}
	
	public void startImportJob(IJavaProject jp) {
		new WaitAndImportJob(jp.getProject(), traceDir, tmpFile);
	}
}
