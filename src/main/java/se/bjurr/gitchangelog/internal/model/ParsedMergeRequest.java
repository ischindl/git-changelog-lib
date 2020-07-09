package se.bjurr.gitchangelog.internal.model;

import java.util.List;

import se.bjurr.gitchangelog.internal.git.model.GitCommit;
import se.bjurr.gitchangelog.internal.model.interfaces.IGitCommitReferer;

public class ParsedMergeRequest implements IGitCommitReferer {

	private GitCommit gitCommit;
	private String name;
	private String description;
	private String link;
	private List<String> labels;

	public ParsedMergeRequest(
								String name,
								String description,
								String link,
								List<String> labels) {
		this.name = name;
		this.description = description;
		this.link = link;
		this.labels = labels;
	}

	@Override
	public GitCommit getGitCommit() {
		return gitCommit;
	}

	public void setGitCommit(GitCommit gitCommit) {
		this.gitCommit = gitCommit;
	}

	@Override
	public String getName() {
		return name;
	}

	public String getDescription() {
		return description;
	}

	public List<String> getLabels() {
		return labels;
	}

	  public String getLink() {
		    return link;
		  }

}
