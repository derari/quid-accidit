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
	
	AcciditLauncher launcher;

	@SuppressWarnings({ "unchecked", "rawtypes" })
	@Override
	protected void collectExecutionArguments(
					ILaunchConfiguration configuration, List vmArguments,
					List programArguments) throws CoreException {
		super.collectExecutionArguments(configuration, vmArguments, programArguments);
		launcher.collectExecutionArguments("test", configuration, vmArguments, programArguments);
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
