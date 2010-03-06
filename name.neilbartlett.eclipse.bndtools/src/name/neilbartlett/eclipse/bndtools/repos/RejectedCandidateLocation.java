package name.neilbartlett.eclipse.bndtools.repos;

public class RejectedCandidateLocation {
	private final IBundleLocation location;
	private final String reason;
	private final boolean cycle;

	public RejectedCandidateLocation(IBundleLocation location, String reason, boolean cycle) {
		this.location = location;
		this.reason = reason;
		this.cycle = cycle;
	}
	public IBundleLocation getLocation() {
		return location;
	}
	public String getReason() {
		return reason;
	}
	public boolean isCycle() {
		return cycle;
	}
}
