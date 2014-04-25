package de.hpi.accidit.launcher;


import java.io.File;
import java.util.Map;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
// TODO fix
//import org.eclipse.jdt.internal.launching.LaunchingMessages;
import org.eclipse.jdt.launching.ExecutionArguments;
import org.eclipse.jdt.launching.IVMRunner;
import org.eclipse.jdt.launching.JavaLaunchDelegate;
import org.eclipse.jdt.launching.VMRunnerConfiguration;
import org.eclipse.osgi.util.NLS;

public class AcciditTracingLaunchConfigurationDelegate extends
JavaLaunchDelegate implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
//		super.launch(configuration, mode, launch, monitor);

		if (monitor == null) {
			monitor = new NullProgressMonitor();
		}

		monitor.beginTask(NLS.bind("{0}...", new String[]{configuration.getName()}), 3); //$NON-NLS-1$
		// check for cancellation
		if (monitor.isCanceled()) {
			return;
		}
		try {
			// TODO: fix
//			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Verifying_launch_attributes____1); 

			String mainTypeName = verifyMainTypeName(configuration);
			IVMRunner runner = getVMRunner(configuration, mode);

			File workingDir = verifyWorkingDirectory(configuration);
			String workingDirName = null;
			if (workingDir != null) {
				workingDirName = workingDir.getAbsolutePath();
			}

			// Environment variables
			String[] envp = getEnvironment(configuration);
			
			

			// Program & VM arguments
			String pgmArgs = getProgramArguments(configuration);
			
			StringBuilder vmArgsBuilder = new StringBuilder();
			vmArgsBuilder.append(getVMArguments(configuration));
			vmArgsBuilder.append(" -Xmx1G");
			vmArgsBuilder.append(" -noverify \"-javaagent:");
			vmArgsBuilder.append(String.format("%s/../lib/accidit-asm-tracer-1.0-SNAPSHOT.jar", workingDirName));
			vmArgsBuilder.append(String.format("=%s/../lib/accidit-tracer-model-1.0-SNAPSHOT.jar", workingDirName));
			vmArgsBuilder.append("\"");
			ExecutionArguments execArgs = new ExecutionArguments(vmArgsBuilder.toString(), pgmArgs);

			// VM-specific attributes
			Map<String, Object> vmAttributesMap = getVMSpecificAttributesMap(configuration);

			// Classpath
			String[] classpath = getClasspath(configuration);

			// Create VM config
			VMRunnerConfiguration runConfig = new VMRunnerConfiguration(mainTypeName, classpath);
			runConfig.setProgramArguments(execArgs.getProgramArgumentsArray());
			runConfig.setEnvironment(envp);
			runConfig.setVMArguments(execArgs.getVMArgumentsArray());
			runConfig.setWorkingDirectory(workingDirName);
			runConfig.setVMSpecificAttributesMap(vmAttributesMap);

			// Bootpath
			runConfig.setBootClassPath(getBootpath(configuration));

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}               

			// stop in main
			prepareStopInMain(configuration);

			// done the verification phase
			monitor.worked(1);

			// TODO: fix
//			monitor.subTask(LaunchingMessages.JavaLocalApplicationLaunchConfigurationDelegate_Creating_source_locator____2); 
			
			// set the default source locator if required
			setDefaultSourceLocator(launch, configuration);
			monitor.worked(1);              

			// Launch the configuration - 1 unit of work
			runner.run(runConfig, launch, monitor);

			// check for cancellation
			if (monitor.isCanceled()) {
				return;
			}       
		}
		finally {
			monitor.done();
		}
	}

}
