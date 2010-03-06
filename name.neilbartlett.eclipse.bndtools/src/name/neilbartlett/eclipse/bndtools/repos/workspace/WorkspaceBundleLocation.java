package name.neilbartlett.eclipse.bndtools.repos.workspace;

import name.neilbartlett.eclipse.bndtools.repos.BaseBundleLocation;
import name.neilbartlett.eclipse.bndtools.repos.IBundleRepository;

import org.eclipse.core.runtime.IPath;

import aQute.libg.version.Version;

class WorkspaceBundleLocation extends BaseBundleLocation {
	
	private final WorkspaceRepository repository;
	
	private final String symbolicName;
	private final Version version;
	private final IPath path;
	private final IPath sourcePath;

	WorkspaceBundleLocation(WorkspaceRepository repository, String symbolicName, Version version, IPath path, IPath sourcePath) {
		this.repository = repository;
		this.symbolicName = symbolicName;
		this.version = version;
		this.path = path;
		this.sourcePath = sourcePath;
	}

	public IBundleRepository getRepository() {
		return repository;
	}

	public String getSymbolicName() {
		return symbolicName;
	}

	public Version getVersion() {
		return version;
	}

	public IPath getPath() {
		return path;
	}

	public IPath getSourcePath() {
		return sourcePath;
	}
}