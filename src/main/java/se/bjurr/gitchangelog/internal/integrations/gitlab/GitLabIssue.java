package se.bjurr.gitchangelog.internal.integrations.gitlab;

import java.util.List;

public class GitLabIssue {

	private final String title;
	private final String desc;
	private final String id;
	private final String link;
	private final List<String> labels;

	public GitLabIssue(String id, String title, String desc, String link, List<String> labels) {
		this.title = title;
		this.link = link;
		this.labels = labels;
		this.desc = desc;
		this.id = id;
	}

	public GitLabIssue(String id, String title, String link, List<String> labels) {
		this(id, title, "", link, labels);
	}

	public List<String> getLabels() {
		return labels;
	}

	public String getLink() {
		return link;
	}

	public String getTitle() {
		return title;
	}

	public String getDesc() {
		return desc;
	}

	public String getId() {
		return id;
	}

	@Override
	public String toString() {
		return "GitLabIssue [id = "+ id +", title=" + title + ", link=" + link + ", labels=" + labels + "]";
	}
}
