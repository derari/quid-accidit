package de.hpi.accidit.launcher;

import java.io.File;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

public class LocalJavaApplicationLaunchDelegate extends JavaLaunchDelegate
		implements ILaunchConfigurationDelegate {
	
	AcciditLauncher launcher;
	
	@Override
	public String getVMArguments(ILaunchConfiguration configuration) throws CoreException {
		String vmArgs = super.getVMArguments(configuration);
		return launcher.getVMArguments("main", configuration, vmArgs);
	}

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		File workingDir = verifyWorkingDirectory(configuration);
		launcher = new AcciditLauncher(workingDir);
		
		super.launch(configuration, mode, launch, monitor);

		IJavaProject jp = getJavaProject(configuration);
		launcher.startImportJob(jp);
    }
}
