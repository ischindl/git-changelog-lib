package se.bjurr.gitchangelog.api.model;

import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import java.io.Serializable;
import java.util.List;

public class MergeRequest implements Serializable {

	private static final long serialVersionUID = -8729720006601829816L;
	private final String name;
	private final String mergeRequest;
	private final String description;
	private final String link;
	private final Commit mergeCommit;
	private final boolean hasLink;
	private final boolean hasDescription;
	private final boolean hasMergeRequest;
	private final List<Commit> commits;

	/**
	 *
	 * @param name
	 * @param mergeRequest
	 * @param description
	 * @param link
	 * @param mergeCommit
	 * @param commits
	 */
	public MergeRequest(String name, String mergeRequest, String description, String link, Commit mergeCommit, List<Commit> commits) {
		this.name = name;
		this.mergeRequest = mergeRequest;
		this.hasMergeRequest = !isNullOrEmpty(mergeRequest);
		this.description = nullToEmpty(description);
		this.hasDescription = !isNullOrEmpty(description);
		this.mergeCommit = mergeCommit;
		this.link = nullToEmpty(link);
		this.hasLink = !isNullOrEmpty(link);
		checkState(!commits.isEmpty(), "MR " + mergeRequest + " have no commits");
		this.commits = commits;
	}

	public Commit getMergeCommit() {
		return mergeCommit;
	}

	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public String getLink() {
		return link;
	}

	public boolean hasLink() {
		return hasLink;
	}

	public boolean hasDescription() {
		return hasDescription;
	}

	@Override
	public String toString() {
		return "name: " + this.name + " desc: " + this.description;
	}

	public String getMergeRequest() {
		return mergeRequest;
	}

	public boolean hasMergeRequest() {
		return hasMergeRequest;
	}

	public List<Commit> getCommits() {
		return commits;
	}
}
