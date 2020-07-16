package se.bjurr.gitchangelog.internal.model;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import se.bjurr.gitchangelog.internal.git.model.GitCommit;
import se.bjurr.gitchangelog.internal.model.interfaces.IGitCommitReferer;

public class ParsedMergeRequest implements IGitCommitReferer {

	private GitCommit gitMergeCommit;
	private String name;
	private String id;
	private String description;
	private String link;
	private List<String> labels;
	private List<GitCommit> gitCommits = newArrayList();
	private List<ParsedIssue> issues = newArrayList();
/**
 *
 * @param id
 * @param name
 * @param description
 * @param link
 * @param labels
 */
	public ParsedMergeRequest(
	                          	String id,
								String name,
								String description,
								String link,
								List<String> labels) {
		this.id = id;
		this.name = name;
		this.description = description;
		this.link = link;
		this.labels = labels;
	}

	@Override
	public GitCommit getGitMergeCommit() {
		return gitMergeCommit;
	}

	public void setGitMergeCommit(GitCommit gitCommit) {
		this.gitMergeCommit = gitCommit;
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

	public void addCommits(List<GitCommit> commits) {
		this.gitCommits.addAll(commits);
	}

	public List<GitCommit> getGitCommits() {
		return gitCommits;
	}

	public String getId() {
		return id;
	}

	public List<ParsedIssue> getIssues() {
		return issues;
	}

}
