package de.hpi.accidit.launcher;

import java.io.File;
import java.sql.SQLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.model.ILaunchConfigurationDelegate;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaLaunchDelegate;

import de.hpi.accidit.db.Import;
import de.hpi.accidit.eclipse.properties.DatabaseSettingsRetriever;

public class LocalJavaApplicationLaunchDelegate extends JavaLaunchDelegate
		implements ILaunchConfigurationDelegate {

	@Override
	public void launch(ILaunchConfiguration configuration, String mode,
			ILaunch launch, IProgressMonitor monitor) throws CoreException {
		
		ILaunchConfigurationWorkingCopy workingCopy = configuration.getWorkingCopy();
		
		File workingDir = verifyWorkingDirectory(configuration);
		File traceDir = null;
		if (workingDir != null) {
			String workingDirName = workingDir.getAbsolutePath();
			traceDir = new File(workingDir, "tmp_trace" + (int)(1000*Math.random()));
			traceDir.mkdirs();
			
			StringBuilder vmArgsBuilder = new StringBuilder();
			vmArgsBuilder.append(getVMArguments(configuration));
			vmArgsBuilder.append(" -Xmx1G");
			vmArgsBuilder.append(" -noverify \"-javaagent:");
			vmArgsBuilder.append(String.format("%s/../lib/accidit-asm-tracer-1.0-SNAPSHOT-jar-with-dependencies.jar", workingDirName));
			vmArgsBuilder.append(String.format("=%s/../lib/accidit-tracer-model-1.0-SNAPSHOT.jar", workingDirName));
			vmArgsBuilder.append("#main#");
			vmArgsBuilder.append(traceDir.getAbsolutePath());
			vmArgsBuilder.append("#0");
			vmArgsBuilder.append("\"");
			
			workingCopy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, vmArgsBuilder.toString());
		}
		
		super.launch(workingCopy, mode, launch, monitor);
		
		//track termination of the program execution
		while (!launch.isTerminated()) {
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) { 
				Thread.currentThread().interrupt();
				break;
			}
		}
		
		if (!launch.isTerminated()) {
			return;
		}
		
		// get database settings
		IProject project = getProject(workingCopy);
		
		String dbAddress = DatabaseSettingsRetriever.getPreferenceValue(project, DatabaseSettingsRetriever.CONNECTION_ADDRESS);
		String dbUser = DatabaseSettingsRetriever.getPreferenceValue(project, DatabaseSettingsRetriever.CONNECTION_USER);
		String dbPassword = DatabaseSettingsRetriever.getPreferenceValue(project, DatabaseSettingsRetriever.CONNECTION_PASSWORD);
		String dbType = DatabaseSettingsRetriever.getPreferenceValue(project, DatabaseSettingsRetriever.CONNECTION_TYPE);
		String dbSchema = DatabaseSettingsRetriever.getPreferenceValue(project, DatabaseSettingsRetriever.CONNECTION_SCHEMA);

		String dbString = String.format("jdbc:%s://%s/%s?user=%s&password=%s&currentschema=%s", dbType, dbAddress, dbSchema, dbUser, dbPassword, dbSchema);		
				
		boolean newSchema = false;		
		
		String csvDir = traceDir.getAbsolutePath();
		
		//start db_import
		try {
			new Import(dbType, dbString, dbSchema, csvDir, newSchema).run();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			for (File f: traceDir.listFiles()) {
				f.delete();
			}
			traceDir.delete();
		}
	}
	
	/**
     * Returns the IProject object matching the name found in the configuration
     * object under the name
     * <code>IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME</code>
     * @param configuration
     * @return The IProject object or null
     */
    private IProject getProject(ILaunchConfiguration configuration){
        String projectName;
        try {
            projectName = configuration.getAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, "");
        } catch (CoreException e) {
            return null;
        }

        IWorkspace workspace = ResourcesPlugin.getWorkspace();
        return workspace.getRoot().getProject(projectName);
    }

}
