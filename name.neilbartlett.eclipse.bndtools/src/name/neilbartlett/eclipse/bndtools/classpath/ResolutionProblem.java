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
import java.util.LinkedList;
import java.util.List;

import name.neilbartlett.eclipse.bndtools.repos.RejectedCandidateLocation;

class ResolutionProblem {
	private final BundleDependency dependency;
	private final String message;
	private final List<RejectedCandidateLocation> rejectedCandidates = new LinkedList<RejectedCandidateLocation>();
	
	public ResolutionProblem(String message, BundleDependency dependency) {
		this.message = message;
		this.dependency = dependency;
	}
	
	public BundleDependency getDependency() {
		return dependency;
	}
	public String getMessage() {
		return message;
	}
	public List<RejectedCandidateLocation> getRejectedCandidates() {
		return rejectedCandidates;
	}
	public void addRejectedCandidate(RejectedCandidateLocation candidate) {
		rejectedCandidates.add(candidate);
	}
	public void addRejectedCandidates(Collection<? extends RejectedCandidateLocation> rejections) {
		rejectedCandidates.addAll(rejections);
	}
}

