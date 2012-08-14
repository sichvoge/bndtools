///*******************************************************************************
// * Copyright (c) 2010 Per Kr. Soreide.
// * All rights reserved. This program and the accompanying materials
// * are made available under the terms of the Eclipse Public License v1.0
// * which accompanies this distribution, and is available at
// * http://www.eclipse.org/legal/epl-v10.html
// *
// * Contributors:
// *     Per Kr. Soreide - initial API and implementation
// *******************************************************************************/
//package bndtools.release;
//
//import java.io.File;
//import java.util.ArrayList;
//import java.util.List;
//
//import org.eclipse.core.runtime.IProgressMonitor;
//import org.eclipse.core.runtime.IStatus;
//import org.eclipse.core.runtime.Status;
//import org.eclipse.core.runtime.jobs.Job;
//import org.eclipse.swt.widgets.Display;
//import org.eclipse.swt.widgets.Shell;
//import org.eclipse.ui.PlatformUI;
//
//import aQute.bnd.build.Project;
//import aQute.bnd.differ.Baseline;
//import aQute.bnd.osgi.Builder;
//import bndtools.release.nl.Messages;
//import bndtools.release.ui.BundleReleaseDialog;
//
//public class ReleaseDialogJob extends Job {
//
//	protected final Shell shell;
//	protected final Project project;
//	private final List<File> subBundles;
//	
//	public ReleaseDialogJob(Project project, List<File> subBundles) {
//		super(Messages.releaseJob);
//		this.project = project;
//		this.shell = PlatformUI.getWorkbench().getDisplay().getActiveShell();
//		this.subBundles = subBundles;
//		setUser(true);
//	}
//
//	@Override
//	protected IStatus run(IProgressMonitor monitor) {
//
//        try {
//	    	monitor.beginTask(Messages.cleaningProject, 100);
//			monitor.setTaskName(Messages.releasing);
//			monitor.worked(33);
//			monitor.subTask(Messages.checkingExported);
//
//			final List<Baseline> diffs = new ArrayList<Baseline>();
//
//			List<Builder> builders = project.getBuilder(null).getSubBuilders();
//			for (Builder builder : builders) {
//
//				if (subBundles != null) {
//					if (!subBundles.contains(builder.getPropertiesFile())) {
//						continue;
//					}
//				}
//
//				Baseline diff = DiffHelper.createBaseline(project, builder.getBsn());
//				if (diff != null) {
//					diffs.add(diff);
//				}
//			}
//			if (diffs.size() == 0) {
//				//TODO: message
//				return Status.OK_STATUS;
//			}
//			monitor.worked(33);
//
//			Runnable runnable = new Runnable() {
//				public void run() {
//					BundleReleaseDialog dialog = new BundleReleaseDialog(shell, project, diffs);
//					dialog.open();
//				}
//			};
//
//			if (Display.getCurrent() == null) {
//				Display.getDefault().asyncExec(runnable);
//			} else {
//				runnable.run();
//			}
//			
//			monitor.worked(33);
//	        return Status.OK_STATUS;
//        } catch (Exception e) {
//        	return new Status(Status.ERROR, Activator.PLUGIN_ID, "Error : " + e.getMessage(), e);
//        } finally {
//        	
//        	monitor.done();
//        }
//
//	}
//	
//}
