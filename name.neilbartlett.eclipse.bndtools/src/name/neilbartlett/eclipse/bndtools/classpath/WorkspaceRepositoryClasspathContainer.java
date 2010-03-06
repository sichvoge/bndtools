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

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import name.neilbartlett.eclipse.bndtools.repos.IBundleLocation;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.jdt.core.IClasspathContainer;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;

class WorkspaceRepositoryClasspathContainer implements
		IClasspathContainer {
	
	private final IPath containerPath;
	private final IJavaProject javaProject;
	private final Collection<BundleDependency> dependencies;
	private final Map<BundleDependency, IBundleLocation> bindings;

	private AtomicReference<IClasspathEntry[]> entriesRef = new AtomicReference<IClasspathEntry[]>(null);

	WorkspaceRepositoryClasspathContainer(IPath containerPath, IJavaProject javaProject, Collection<BundleDependency> dependencies, Map<BundleDependency,IBundleLocation> bindings) {
		this.containerPath = containerPath;
		this.javaProject = javaProject;
		this.dependencies = dependencies;
		this.bindings = bindings;
	}
	public IClasspathEntry[] getClasspathEntries() {
		IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
		
		IClasspathEntry[] result = entriesRef.get();
		if(result != null) return result;
		
		result = new IClasspathEntry[bindings.size()];
		int i = 0;
		for(Iterator<IBundleLocation> iter = bindings.values().iterator(); iter.hasNext(); i++) {
			IBundleLocation location = iter.next();
			
			IPath bndFilePath = location.getSourcePath();
			IPath srcProjectPath = null;
			if(bndFilePath != null) {
				IResource bndFile = root.findMember(bndFilePath);
				if(bndFile != null) {
					srcProjectPath = bndFile.getProject().getFullPath();
				}
			}
			
			result[i] = JavaCore.newLibraryEntry(location.getPath(), srcProjectPath, null, false);
		}
		entriesRef.compareAndSet(null, result);
		return entriesRef.get();
	}
	public String getDescription() {
		return "Workspace Bundle Repository";
	}
	public int getKind() {
		return K_APPLICATION;
	}
	public IPath getPath() {
		return containerPath;
	}
	public Collection<BundleDependency> getDependencies() {
		return Collections.unmodifiableCollection(dependencies);
	}
	public IBundleLocation getBinding(BundleDependency dependency) {
		return bindings.get(dependency);
	}
	public Map<BundleDependency, IBundleLocation> getAllBindings() {
		return Collections.unmodifiableMap(bindings);
	}
	public boolean isBoundToPath(IPath path) {
		for (IBundleLocation location : bindings.values()) {
			if(location.getPath().equals(path)) {
				return true;
			}
		}
		return false;
	}
	public IJavaProject getJavaProject() {
		return javaProject;
	}
}
