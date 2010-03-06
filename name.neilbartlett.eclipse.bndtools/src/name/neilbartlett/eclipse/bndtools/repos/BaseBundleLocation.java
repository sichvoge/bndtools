package name.neilbartlett.eclipse.bndtools.repos;

import aQute.libg.version.Version;


public abstract class BaseBundleLocation implements IBundleLocation {

	public final int compareTo(IBundleLocation otherLoc) {
		int diff = this.getSymbolicName().compareTo(otherLoc.getSymbolicName());
		if(diff == 0) {
			Version version1 = this.getVersion();
			if(version1 == null) version1 = new Version(0);
			Version version2 = otherLoc.getVersion();
			if(version2 == null) version2 = new Version(0);
			
			diff = version1.compareTo(version2);
		}
		return diff;
	}

}
