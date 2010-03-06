package name.neilbartlett.eclipse.bndtools.repos.workspace;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicReference;

import name.neilbartlett.eclipse.bndtools.classpath.BundleDependency;
import name.neilbartlett.eclipse.bndtools.classpath.WorkspaceRepositoryClasspathContainerInitializer;
import name.neilbartlett.eclipse.bndtools.repos.IBundleLocation;
import name.neilbartlett.eclipse.bndtools.repos.IBundleRepository;
import name.neilbartlett.eclipse.bndtools.repos.RejectedCandidateLocation;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;

import aQute.libg.version.Version;

public class WorkspaceRepository implements IBundleRepository {
	
	// STATIC SECTION
	
	private static final AtomicReference<WorkspaceRepository> instanceRef = new AtomicReference<WorkspaceRepository>(null);

	public static final WorkspaceRepository getInstance() {
		WorkspaceRepository instance;

		instance = instanceRef.get();
		if (instance == null) {
			instanceRef.compareAndSet(null, new WorkspaceRepository());
			instance = instanceRef.get();
		}
		return instanceRef.get();
	}
	
	// INSTANCE SECTION
	
	/** The bundles available in the workspace. <b>Map{bsn -> bundle list}</b> */
	private final Map<String, List<WorkspaceBundleLocation>> workspaceBundles = new HashMap<String, List<WorkspaceBundleLocation>>();
	
	/** The bundles exported by each project. <b>Map{project name -> Map{bundle path -> bundle}}</b> */
	private final Map<String, Map<IPath,WorkspaceBundleLocation>> exportsMap = new HashMap<String, Map<IPath,WorkspaceBundleLocation>>();

	/*
	 * Prevents instantiation
	 */
	private WorkspaceRepository() {
	}

	public void findCandidates(IProject project, BundleDependency dependency, Collection<? super IBundleLocation> candidates, Collection<? super RejectedCandidateLocation> rejections) {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();

		List<WorkspaceBundleLocation> locations = workspaceBundles.get(dependency.getSymbolicName());
		if(locations != null) {
			for (WorkspaceBundleLocation location : locations) {
				Version version = location.getVersion();
				if(dependency.getVersionRange().includes(version)) {
					if(!checkForCycles(root, project, location)) {
						candidates.add(location);
					} else {
						rejections.add(new RejectedCandidateLocation(location, "Possible dependency cycle", true));
					}
				}
			}
		}
	}
	/**
	 * Process a set of changes to the exported bundles for a project.
	 * 
	 * @param project
	 *            The project whose exported bundles may be changing.
	 * @param deletedJarFiles
	 *            A list of JAR files in the project, which may or may not be
	 *            bundles that are currently exported by the project.
	 * @param changedBundles
	 *            A list of changed or added bundles in the project
	 * @param monitor
	 *            the progress monitor to use for reporting progress to the
	 *            user. It is the caller's responsibility to call done() on the
	 *            given monitor. Accepts null, indicating that no progress
	 *            should be reported and that the operation cannot be cancelled.
	 * @return
	 */
	public IStatus bundlesChanged(IProject project, List<IFile> deletedJarFiles, List<ExportedBundle> changedBundles, IProgressMonitor monitor) {
		// Calculate changed and added locations
		List<IBundleLocation> addedLocations = new LinkedList<IBundleLocation>();
		List<IBundleLocation> changedLocations = new LinkedList<IBundleLocation>();
		Map<IPath, WorkspaceBundleLocation> exportedBundles = exportsMap.get(project.getName());
		if(changedBundles != null && !changedBundles.isEmpty()) {
			if(exportedBundles == null) {
				exportedBundles = new HashMap<IPath, WorkspaceBundleLocation>();
				exportsMap.put(project.getName(), exportedBundles);
			}
			for (ExportedBundle bundle : changedBundles) {
				WorkspaceBundleLocation location = new WorkspaceBundleLocation(this, bundle.getSymbolicName(), bundle.getVersion(), bundle.getPath(), bundle.getSourceBndFilePath());
				WorkspaceBundleLocation priorLocation = exportedBundles.put(location.getPath(), location);
				if(priorLocation == null) {
					addBundleLocation(location);
					
					addedLocations.add(location);
				} else {
					removeBundleLocation(priorLocation);
					addBundleLocation(location);
					
					changedLocations.add(location);
				}
			}
		}
		
		// Calculate deleted locations
		List<IBundleLocation> removedLocations = new LinkedList<IBundleLocation>();
		if(deletedJarFiles != null) {
			for (IFile file : deletedJarFiles) {
				if(exportedBundles != null) {
					WorkspaceBundleLocation removedLocation = exportedBundles.remove(file.getFullPath());
					if(removedLocation != null) {
						removedLocations.add(removedLocation);
						removeBundleLocation(removedLocation);
					}
					if(exportedBundles.isEmpty()) {
						exportsMap.remove(project.getName());
					}
				}
			}
		}
		
		return WorkspaceRepositoryClasspathContainerInitializer.getInstance().bundleLocationsChanged(project, addedLocations, changedLocations, removedLocations, monitor);
	}
	private void addBundleLocation(WorkspaceBundleLocation location) {
		// Add to the workspace bundles
		List<WorkspaceBundleLocation> list = workspaceBundles.get(location.getSymbolicName());
		if(list == null) {
			list = new LinkedList<WorkspaceBundleLocation>();
			workspaceBundles.put(location.getSymbolicName(), list);
		}
		list.add(location);
	}
	private void removeBundleLocation(WorkspaceBundleLocation location) {
		List<WorkspaceBundleLocation> list = workspaceBundles.get(location.getSymbolicName());
		if(list != null) {
			list.remove(location);
			if(list.isEmpty()) {
				workspaceBundles.remove(location.getSymbolicName());
			}
		}
	}
	/**
	 * Set or reset the exports for the specified project.
	 * 
	 * @param project
	 *            The project whose exported bundles are being reset.
	 * @param bundles
	 *            A list of changed or added bundles in the project
	 * @param monitor
	 *            the progress monitor to use for reporting progress to the
	 *            user. It is the caller's responsibility to call done() on the
	 *            given monitor. Accepts null, indicating that no progress
	 *            should be reported and that the operation cannot be cancelled.
	 * @return The status of the operation.
	 */
	public IStatus resetProjectExports(IProject project, List<ExportedBundle> bundles, IProgressMonitor monitor) {
		List<IBundleLocation> removedLocations = new LinkedList<IBundleLocation>();
		removeAllExports(project, removedLocations);
		
		// Calculate changed and added locations
		List<IBundleLocation> addedLocations = new LinkedList<IBundleLocation>();
		List<IBundleLocation> changedLocations = new LinkedList<IBundleLocation>();
		Map<IPath, WorkspaceBundleLocation> exportedBundles = exportsMap.get(project.getName());
		if(bundles != null && !bundles.isEmpty()) {
			if(exportedBundles == null) {
				exportedBundles = new HashMap<IPath, WorkspaceBundleLocation>();
				exportsMap.put(project.getName(), exportedBundles);
			}
			for (ExportedBundle bundle : bundles) {
				WorkspaceBundleLocation location = new WorkspaceBundleLocation(this, bundle.getSymbolicName(), bundle.getVersion(), bundle.getPath(), bundle.getSourceBndFilePath());
				WorkspaceBundleLocation priorLocation = exportedBundles.put(location.getPath(), location);
				if(priorLocation == null) {
					addBundleLocation(location);

					addedLocations.add(location);
				} else {
					removeBundleLocation(priorLocation);
					addBundleLocation(location);

					changedLocations.add(location);
				}
			}
		}
		
		return WorkspaceRepositoryClasspathContainerInitializer.getInstance().bundleLocationsChanged(project, addedLocations, changedLocations, removedLocations, monitor);
	}
	private void removeAllExports(IProject project, Collection<? super WorkspaceBundleLocation> removedLocations) {
		Map<IPath, WorkspaceBundleLocation> exportedBundles = exportsMap.remove(project.getName());
		if(exportedBundles != null) {
			for (WorkspaceBundleLocation removedLoc : exportedBundles.values()) {
				removeBundleLocation(removedLoc);

				removedLocations.add(removedLoc);
			}
		}
	}
	public List<WorkspaceBundleLocation> getAllBundles() {
		List<WorkspaceBundleLocation> result = new ArrayList<WorkspaceBundleLocation>();
		for(Entry<String, Map<IPath, WorkspaceBundleLocation>> entry : exportsMap.entrySet()) {
			Map<IPath, WorkspaceBundleLocation> pathMap = entry.getValue();
			for (Entry<IPath, WorkspaceBundleLocation> pathEntry : pathMap.entrySet()) {
				result.add(pathEntry.getValue());
			}
		}
		return result;
	}
	/* TODO Remove
	private void processChangedBundles(IProject project, List<ExportedBundle> changedBundles, Collection<? super String> affectedProjects) {
		Map<IPath, ExportedBundle> exportedBundles = exportsMap.get(project.getName());
		if(changedBundles != null && !changedBundles.isEmpty()) {
			if(exportedBundles == null) {
				exportedBundles = new HashMap<IPath, ExportedBundle>();
				exportsMap.put(project.getName(), exportedBundles);
			}
			for(ExportedBundle changedBundle : changedBundles) {
				ExportedBundle priorEntry = exportedBundles.put(changedBundle.getPath(), changedBundle);
				if(priorEntry == null) {
					affectedProjects.addAll(addBundle(changedBundle));
				} else {
					if(!priorEntry.getSymbolicName().equals(changedBundle.getSymbolicName()) || !priorEntry.getVersion().equals(changedBundle.getVersion())) {
						affectedProjects.addAll(removeBundle(priorEntry));
						affectedProjects.addAll(addBundle(changedBundle));
					}
				}
			}
		}
	}
	private void processDeletedJarFiles(IProject project, List<IFile> deletedJarFiles, Collection<? super String> affectedProjects) {
		List<IBundleLocation> removedLocations = new LinkedList<IBundleLocation>();
		
		Map<IPath, WorkspaceBundleLocation> exportedBundles = exportsMap.get(project.getName());
		if(deletedJarFiles != null) {
			for (IFile deletedJarFile : deletedJarFiles) {
				if(exportedBundles != null) {
					WorkspaceBundleLocation removedLocation = exportedBundles.remove(deletedJarFile.getFullPath());
					if(removedLocation != null) {
						removeLocation(removedLocation);
						removedLocations.add(removedLocation);
					}
					if(exportedBundles.isEmpty()) {
						exportsMap.remove(project.getName());
					}
				}
			}
		}
		
		WorkspaceRepositoryClasspathContainerInitializer.getInstance().removedLocations(removedLocations);
	}
	*/
	
	boolean checkForCycles(IWorkspaceRoot root, IProject startingProject, WorkspaceBundleLocation location) {

		IPath bndPath = location.getSourcePath();

		// If no source bnd file then this bundle cannot be part of a cycle, since it is a pre-built binary
		if(bndPath == null) {
			return false;
		}
		// If the bndFile is the starting project, then this export would directly result in a cycle
		if(startingProject.getName().equals(bndPath.segment(0))) {
			return true;
		}
		// Find the bindings of the project exporting the bundle, and recurse
		IProject project = root.getProject(bndPath.segment(0));
		Map<BundleDependency, IBundleLocation> transitiveBindings = WorkspaceRepositoryClasspathContainerInitializer.getInstance().getBindingsForProject(project);
		if(transitiveBindings != null) {
			for (IBundleLocation dependency : transitiveBindings.values()) {
				if(dependency instanceof WorkspaceBundleLocation) {
					if(checkForCycles(root, startingProject, (WorkspaceBundleLocation) dependency)) {
						return true;
					}
				}
			}
		}
		return false;
	}
	

}
