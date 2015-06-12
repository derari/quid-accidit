package de.hpi.accidit.launcher;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.junit.launcher.JUnitLaunchConfigurationDelegate;

import de.hpi.accidit.eclipse.DatabaseConnector;

public class JUnitLaunchDelegate extends JUnitLaunchConfigurationDelegate
		implements ILaunchConfigurationDelegate {
	
	File traceDir;
	File tmpFile;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void collectExecutionArguments(
					ILaunchConfiguration configuration, List vmArguments,
					List programArguments) throws CoreException {
		super.collectExecutionArguments(configuration, vmArguments, programArguments);
		if (!vmArguments.stream().anyMatch(s -> ((String) s).startsWith("-Xmx"))) {
			vmArguments.add("-Xmx1G");
		}
		vmArguments.add("-noverify");
		
		File workingDir = verifyWorkingDirectory(configuration);
		String workingDirName = workingDir.getAbsolutePath();
		traceDir = new File(workingDir, "accidit_trace");
		
		String agentArg = 
				"-javaagent:"
				+ String.format("%s/../lib/accidit-asm-tracer-1.0-SNAPSHOT.jar", workingDirName)
				+ String.format("=%s/../lib/accidit-tracer-model-1.0-SNAPSHOT.jar", workingDirName)
				+ "#test#"
				+ traceDir.getAbsolutePath()
				+ "#n#"
				+ tmpFile.getAbsolutePath();
		vmArguments.add(agentArg);
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		try {
			tmpFile = File.createTempFile("accidit_canary", ".log");
			tmpFile.delete();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
		System.out.println(tmpFile);
		
		IJavaProject jp = getJavaProject(configuration);
		
		super.launch(configuration, mode, launch, monitor);
		
		new WaitAndImportJob(jp.getProject(), traceDir, tmpFile);
	}	
}
