package se.bjurr.gitchangelog.internal.integrations.gitlab;

import java.util.List;

public class GitLabMergeRequest {

  private final String title;
  private final String description;
  private final String mergeCommitSha;
  private final String link;
  private final List<String> labels;

  public GitLabMergeRequest(String mergeCommitSha, String title, String desc, String link, List<String> labels) {
    this.title = title;
    this.link = link;
    this.labels = labels;
    this.description = desc;
    this.mergeCommitSha = mergeCommitSha;
  }

  public GitLabMergeRequest(String mergeCommitSha, String title, String link, List<String> labels) {
	    this(mergeCommitSha, title, "", link, labels);
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
}
