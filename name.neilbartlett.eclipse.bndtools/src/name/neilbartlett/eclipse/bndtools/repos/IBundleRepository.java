package name.neilbartlett.eclipse.bndtools.repos;

import java.util.Collection;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.classpath.BundleDependency;

import org.eclipse.core.resources.IProject;

public interface IBundleRepository {

	void findCandidates(IProject project, BundleDependency dependency, Collection<? super IBundleLocation> candidates, Collection<? super RejectedCandidateLocation> rejections);
	List<? extends IBundleLocation> getAllBundles();
	
}
