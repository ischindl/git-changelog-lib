package se.bjurr.gitchangelog.api.model;

import static com.google.common.base.Strings.isNullOrEmpty;
import static com.google.common.base.Strings.nullToEmpty;

import java.io.Serializable;

public class MergeRequest implements Serializable {

	private static final long serialVersionUID = -8729720006601829816L;
	private final String name;
	private final String description;
	private final String link;
	private final Commit commit;
	private final boolean hasLink;
	private final boolean hasDescription;

	public MergeRequest(String name, String description, String link, Commit commit) {
		this.name = name;
	    this.description = nullToEmpty(description);
	    this.hasDescription = !isNullOrEmpty(description);
		this.commit = commit;
		this.link = nullToEmpty(link);
		this.hasLink = !isNullOrEmpty(link);
	}

	public Commit getCommit() {
		return commit;
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
		return "name: " + this.name + " desc: "+this.description;
	}
}
