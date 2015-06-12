package de.hpi.accidit.launcher;

import java.io.File;
import java.sql.SQLException;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.hpi.accidit.db.Import;
import de.hpi.accidit.eclipse.DatabaseConnector;

public class WaitAndImportJob extends Job {
	
	private IProject project;
	private File csvDir;
	private File canaryFile;
	private int c = 60*60*2;
	
	public WaitAndImportJob(IProject project, File csvDir, File canaryFile) {
		super("Accidit Import");
		this.project = project;
		this.csvDir = csvDir;
		this.canaryFile = canaryFile;
		schedule(500);
	}

	@Override
	protected IStatus run(IProgressMonitor monitor) {
		if (!canaryFile.exists()) {
			if (c-- > 0) {
				schedule(500);
			}
			return Status.CANCEL_STATUS;
		}
		canaryFile.delete();
		monitor.beginTask("Importing Trace Data", 2);
		
		DatabaseConnector.setSelectedProject(project);
		String dbString = DatabaseConnector.getDBString();
		
		monitor.worked(1);
		try {
			new Import(dbString, csvDir.getAbsolutePath(), true).run();
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		monitor.worked(1);
		monitor.done();
		
		return Status.OK_STATUS;
	}
}
