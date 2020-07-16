package se.bjurr.gitchangelog.internal.integrations.gitlab;

import static com.google.common.collect.Lists.newArrayList;

import java.util.List;

import se.bjurr.gitchangelog.api.model.Issue;
import se.bjurr.gitchangelog.internal.git.model.GitCommit;


public class GitLabMergeRequest {

  private final String title;
  private final String id;
  private final String description;
  private final String mergeCommitSha;
  private final String link;
  private final List<String> labels;
  private final List<GitCommit> commits = newArrayList();
  private final List<GitLabIssue> closesIssues = newArrayList();


  public GitLabMergeRequest(String id, String title, String desc, String mergeCommitSha, String link, List<String> labels) {
    this.title = title;
    this.id = id;
    this.link = link;
    this.labels = labels;
    this.description = desc;
    this.mergeCommitSha = mergeCommitSha;
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

  public String getDescription() {
		return description;
	}

  public String getMergeCommitSha() {
	return mergeCommitSha;
  }

  @Override
  public String toString() {
    return "GitLabMergeRequest [title=" + title + ", link=" + link + ", labels=" + labels + "]";
  }

public List<GitCommit> getCommits() {
	return commits;
}

public String getId() {
	return id;
}

public List<GitLabIssue> getClosesIssues() {
	return closesIssues;
}
}
