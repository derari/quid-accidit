package de.hpi.accidit.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class LocalJavaApplicationLaunchDelegate extends JavaLaunchDelegate
		implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
		
		File workingDir = verifyWorkingDirectory(configuration);
		if (workingDir != null) {
			String workingDirName = workingDir.getAbsolutePath();
			
			StringBuilder vmArgsBuilder = new StringBuilder();
			vmArgsBuilder.append(getVMArguments(configuration));
			vmArgsBuilder.append(" -Xmx1G");
			vmArgsBuilder.append(" -noverify \"-javaagent:");
			vmArgsBuilder.append(String.format("%s/../lib/accidit-asm-tracer-1.0-SNAPSHOT-jar-with-dependencies.jar", workingDirName));
			vmArgsBuilder.append(String.format("=%s/../lib/accidit-tracer-model-1.0-SNAPSHOT.jar", workingDirName));
			vmArgsBuilder.append("\"");
			
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgsBuilder.toString());
		}
		
		super.launch(workingCopy, mode, launch, monitor);
		
	}

}
