package se.bjurr.gitchangelog.internal.integrations.gitlab;

import java.util.List;

public class GitLabIssue {

  private final String title;
  private final String desc;

private final String link;
  private final List<String> labels;

  public GitLabIssue(String title, String desc, String link, List<String> labels) {
    this.title = title;
    this.link = link;
    this.labels = labels;
    this.desc = desc;
  }

  public GitLabIssue(String title, String link, List<String> labels) {
	    this(title, "", link, labels);
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

  @Override
  public String toString() {
    return "GitLabIssue [title=" + title + ", link=" + link + ", labels=" + labels + "]";
  }
}
