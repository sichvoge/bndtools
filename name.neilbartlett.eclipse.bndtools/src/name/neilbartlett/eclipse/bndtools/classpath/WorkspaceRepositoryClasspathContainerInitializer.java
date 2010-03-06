/*******************************************************************************
 * Copyright (c) 2010 Neil Bartlett.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Neil Bartlett - initial API and implementation
 ******************************************************************************/
package name.neilbartlett.eclipse.bndtools.classpath;

import java.io.IOException;
import java.text.MessageFormat;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import name.neilbartlett.eclipse.bndtools.Plugin;
import name.neilbartlett.eclipse.bndtools.project.BndProjectProperties;
import name.neilbartlett.eclipse.bndtools.repos.IBundleLocation;
import name.neilbartlett.eclipse.bndtools.repos.IBundleRepository;
import name.neilbartlett.eclipse.bndtools.repos.RejectedCandidateLocation;
import name.neilbartlett.eclipse.bndtools.repos.workspace.WorkspaceRepository;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubMonitor;
import org.eclipse.jdt.core.ClasspathContainerInitializer;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.JavaModelException;

public class WorkspaceRepositoryClasspathContainerInitializer extends
		ClasspathContainerInitializer {
	
	// STATIC SECTION
	
	public static final String CONTAINER_ID = Plugin.PLUGIN_ID + ".WORKSPACE_REPOSITORY";
	
	private static final AtomicReference<WorkspaceRepositoryClasspathContainerInitializer> instanceRef
		= new AtomicReference<WorkspaceRepositoryClasspathContainerInitializer>(null);
	
	public static final WorkspaceRepositoryClasspathContainerInitializer getInstance() {
		WorkspaceRepositoryClasspathContainerInitializer instance;

		instance = instanceRef.get();
		if(instance == null) {
			instanceRef.compareAndSet(null, new WorkspaceRepositoryClasspathContainerInitializer());
			instance = instanceRef.get();
		}
		return instanceRef.get();
	}
	
	// INSTANCE SECTION
	
	/** The containers that have been previously configured against the projects. <b>Map{project name -> container}</b> **/
	private final Map<String, WorkspaceRepositoryClasspathContainer> projectContainerMap = new HashMap<String, WorkspaceRepositoryClasspathContainer>();
	
	private final List<IBundleRepository> repositories = new LinkedList<IBundleRepository>();
	
	// Prevent instantiation.
	private WorkspaceRepositoryClasspathContainerInitializer() {
		// TODO: setup additional repositories
		repositories.add(WorkspaceRepository.getInstance());
	}
	
	@Override
	public void initialize(IPath containerPath, IJavaProject project) throws CoreException {
		if(containerPath.segmentCount() >= 1 && CONTAINER_ID.equals(containerPath.segment(0))) {
			// Construct the new container
			BndProjectProperties projectProps = new BndProjectProperties(project.getProject());
			try {
				projectProps.load();
			} catch (IOException e) {
				throw new CoreException(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, "Error loading Bnd project properties.", e));
			}
			
			List<BundleDependency> dependencies = projectProps.getBundleDependencies();
			Map<BundleDependency, IBundleLocation> bindings = calculateBindings(project, dependencies);
			WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(containerPath, project, dependencies, bindings);
			projectContainerMap.put(project.getProject().getName(), newContainer);
			
			// Rebind the container path for the project
			JavaCore.setClasspathContainer(containerPath, new IJavaProject[] { project }, new IClasspathContainer[] { newContainer }, null);
		}
	}
	private Map<BundleDependency, IBundleLocation> calculateBindings(IJavaProject project, Collection<? extends BundleDependency> dependencies) {
		ClasspathProblemReporterJob markerUpdateJob = new ClasspathProblemReporterJob(project);
		
		Map<BundleDependency,IBundleLocation> bindings = new HashMap<BundleDependency,IBundleLocation>();
		for (BundleDependency dependency : dependencies) {
			TreeSet<IBundleLocation> matches = new TreeSet<IBundleLocation>(new Comparator<IBundleLocation>() {
				// Rank by version
				public int compare(IBundleLocation o1, IBundleLocation o2) {
					return o1.getVersion().compareTo(o2.getVersion());
				}
			});
			List<RejectedCandidateLocation> rejections = new LinkedList<RejectedCandidateLocation>();
			
			for (IBundleRepository repository : repositories) {
				repository.findCandidates(project.getProject(), dependency, matches, rejections);
				
				// Take the lowest-version available export
				if(!matches.isEmpty()) {
					IBundleLocation lowestMatch = matches.first();
					bindings.put(dependency, lowestMatch);
				} else {
					String message = MessageFormat.format("No available exports matching the version range \"{0}\". {1,choice,0#No candidates|1#One candidate|1<{1} candidates} rejected.", dependency.getVersionRange(), rejections.size());
					ResolutionProblem problem = new ResolutionProblem(message, dependency);
					problem.addRejectedCandidates(rejections);
					
					markerUpdateJob.addResolutionProblem(problem);
				}
			}
		}
		markerUpdateJob.schedule();
		
		return bindings;
	}

	/**
	 * Get all the dependency bindings for the specified project
	 * 
	 * @param project
	 *            The project to examine
	 * @return A map of dependencies to bindings for those dependencies. The map
	 *         will not contain any entries for unbound dependencies, so they
	 *         must be discovered by another means.
	 */
	public Map<BundleDependency, IBundleLocation> getBindingsForProject(IProject project) {
		WorkspaceRepositoryClasspathContainer container = projectContainerMap.get(project.getName());
		if(container == null) {
			return Collections.emptyMap();
		}
		return container.getAllBindings();
	}
	
	public IStatus bundleLocationsChanged(IProject project, List<IBundleLocation> addedLocations, List<IBundleLocation> changedLocations, List<IBundleLocation> removedLocations, IProgressMonitor monitor) {
		Set<String> affectedProjects = new HashSet<String>();
		
		// Process additions
		if(addedLocations != null) {
			for (IBundleLocation location : addedLocations) {
				addBundleLocation(location, affectedProjects);
			}
		}
		// Process changes
		if(changedLocations != null) {
			for (IBundleLocation location : changedLocations) {
				removeBundleLocation(location, affectedProjects);
				addBundleLocation(location, affectedProjects);
			}
		}
		// Process removals
		if(removedLocations != null) {
			for (IBundleLocation location : removedLocations) {
				removeBundleLocation(location, affectedProjects);
			}
		}
		
		if(!affectedProjects.isEmpty())
			return updateClasspathContainers(affectedProjects, monitor);
		
		return Status.OK_STATUS;
	}
	
	void addBundleLocation(IBundleLocation location, Collection<String> affectedProjects) {
		for (Entry<String, WorkspaceRepositoryClasspathContainer> projectEntry: projectContainerMap.entrySet()) {
			String projectName = projectEntry.getKey();
			WorkspaceRepositoryClasspathContainer container = projectEntry.getValue();
			for (BundleDependency dependency : container.getDependencies()) {
				if(dependency.getSymbolicName().equals(location.getSymbolicName()) && dependency.getVersionRange().includes(location.getVersion())) {
					// Interesting if not already bound to an equal or higher version
					IBundleLocation boundLocation = container.getBinding(dependency);
					if(boundLocation == null || boundLocation.getVersion().compareTo(location.getVersion()) >= 0) {
						affectedProjects.add(projectName);
						break;
					}
				}
			}
		}
	}
	void removeBundleLocation(IBundleLocation location, Collection<String> affectedProjects) {
		for (Entry<String, WorkspaceRepositoryClasspathContainer> entry : projectContainerMap.entrySet()) {
			String projectName = entry.getKey();
			if(entry.getValue().isBoundToPath(location.getPath())) {
				affectedProjects.add(projectName);
			}
		}
	}

	// Fix the classpath containers for the affected projects
	private IStatus updateClasspathContainers(Collection<String> affectedProjects, IProgressMonitor monitor) {
		MultiStatus status = new MultiStatus(Plugin.PLUGIN_ID, 0, "Error(s) occurred processing change to exported bundles.", null);

		SubMonitor progress = SubMonitor.convert(monitor, affectedProjects.size());
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		for (String projectName : affectedProjects) {
			IProject dependerProject = root.getProject(projectName);
			IJavaProject javaProject = JavaCore.create(dependerProject);
			
			WorkspaceRepositoryClasspathContainer oldContainer = projectContainerMap.get(projectName);
			Collection<BundleDependency> dependencies = oldContainer.getDependencies();
			Map<BundleDependency, IBundleLocation> newBindings = calculateBindings(javaProject, dependencies);
			
			WorkspaceRepositoryClasspathContainer newContainer = new WorkspaceRepositoryClasspathContainer(oldContainer.getPath(), oldContainer.getJavaProject(), dependencies, newBindings);
			projectContainerMap.put(projectName, newContainer);
			
			try {
				JavaCore.setClasspathContainer(newContainer.getPath(), new IJavaProject[] { javaProject }, new IClasspathContainer[] { newContainer }, progress.newChild(1));
			} catch (JavaModelException e) {
				status.add(new Status(IStatus.ERROR, Plugin.PLUGIN_ID, 0, MessageFormat.format("Failed to set classpath for project '{0}'", projectName), e));
			}
		}
		
		return status;
	}

	/**
	 * Add the bundle to the workspace
	 * 
	 * @param bundle
	 *            The bundle that is being added
	 * @return The set of projects that may be affected by this addition, e.g.
	 *         they should import the new bundle.
	 */
	/* TODO: remove
	private Set<String> addBundle(ExportedBundle bundle) {
		// Add to the workspace bundles
		Map<Version, SortedSet<IPath>> versionMap = workspaceBundleMap.get(bundle.getSymbolicName());
		if(versionMap == null) {
			versionMap = new HashMap<Version, SortedSet<IPath>>();
			workspaceBundleMap.put(bundle.getSymbolicName(), versionMap);
		}
		SortedSet<IPath> paths = versionMap.get(bundle.getVersion());
		if(paths == null) {
			paths = new TreeSet<IPath>(new Comparator<IPath>() {
				public int compare(IPath o1, IPath o2) {
					return o1.toString().compareTo(o2.toString());
				}
			});
			versionMap.put(bundle.getVersion(), paths);
		}
		paths.add(bundle.getPath());
		
		// Work out which projects are affected (i.e. should import the new bundle)
		Set<String> affectedProjects = new HashSet<String>();
		for (Entry<String, WorkspaceRepositoryClasspathContainer> projectEntry: projectContainerMap.entrySet()) {
			String projectName = projectEntry.getKey();
			WorkspaceRepositoryClasspathContainer container = projectEntry.getValue();
			for (BundleDependency dependency : container.getDependencies()) {
				if(dependency.getSymbolicName().equals(bundle.getSymbolicName()) && dependency.getVersionRange().includes(bundle.getVersion())) {
					// Interesting if not already bound to an equal or lower version
					ExportedBundle boundExport = container.getBinding(dependency);
					if(boundExport == null || boundExport.getVersion().compareTo(bundle.getVersion()) >= 0) {
						affectedProjects.add(projectName);
						break;
					}
				}
			}
		}
		return affectedProjects;
	}
	*/
}
