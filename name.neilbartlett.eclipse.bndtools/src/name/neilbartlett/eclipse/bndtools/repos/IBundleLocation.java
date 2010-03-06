package name.neilbartlett.eclipse.bndtools.repos;

import org.eclipse.core.runtime.IPath;

import aQute.libg.version.Version;

public interface IBundleLocation extends Comparable<IBundleLocation> {
	
	IBundleRepository getRepository();
	
	String getSymbolicName();
	Version getVersion();
	IPath getSourcePath();
	IPath getPath();
}
