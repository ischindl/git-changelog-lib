package se.bjurr.gitchangelog.internal.issues;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.bjurr.gitchangelog.api.exceptions.GitChangelogIntegrationException;
import se.bjurr.gitchangelog.internal.git.model.GitCommit;
import se.bjurr.gitchangelog.internal.integrations.gitlab.GitLabClient;
import se.bjurr.gitchangelog.internal.integrations.gitlab.GitLabIssue;
import se.bjurr.gitchangelog.internal.integrations.gitlab.GitLabMergeRequest;
import se.bjurr.gitchangelog.internal.model.ParsedIssue;
import se.bjurr.gitchangelog.internal.model.ParsedMergeRequest;
import se.bjurr.gitchangelog.internal.settings.Settings;
import se.bjurr.gitchangelog.internal.settings.SettingsIssueType;

public class MergeRequestParser {

	private static final Logger LOG = LoggerFactory.getLogger(MergeRequestParser.class);

	private final List<GitCommit> commits;
	private final Map<String, ParsedIssue> issues = new HashMap<>();
	private final Settings settings;

	public MergeRequestParser(final Settings settings, final List<GitCommit> commits) {
		this.settings = settings;
		this.commits = commits;
	}

	public Settings getSettings() {
		return settings;
	}

	public List<GitCommit> getCommits() {
		return commits;
	}

	/**
	 * extract the oldest date from commits, retrieve mergeRequests by this date and attach merge commits to their
	 * @return
	 */
	public List<ParsedMergeRequest> getMergeRequests() {
		final GitLabClient gitLabClient = createGitLabClient();

		List<ParsedMergeRequest> parsedRequests = new ArrayList<>();
		List<GitCommit> otherCommits = new ArrayList<>();
		otherCommits.addAll(commits);
		Date fromDate = Collections.min(commits, Comparator.comparing(commit -> commit.getCommitTime())).getCommitTime();
		List<GitLabMergeRequest> requests;
		if (gitLabClient != null) {
			try {
				requests = gitLabClient.getAllMergeRequests(settings.getGitLabProjectName().get(), fromDate, settings.getForBranch().get());
				for (GitLabMergeRequest mr : requests) {
					ParsedMergeRequest parsedMR = createParsedMergeRequest(mr);
					Optional<GitCommit> commit = commits.stream().filter(e -> e.getHash().equals(mr.getMergeCommitSha())).findFirst();
					if (commit.isPresent()) {
						parsedMR.setGitMergeCommit(commit.get());
						otherCommits.remove(commit.get());
					}else {
						// if no merge commit request, is not interested
						LOG.info("MR "+ mr.getId() + " has not merge commit");
						continue;
					}
					if(mr.getCommits().size()>0) {
						parsedMR.getGitCommits().addAll(mr.getCommits());
						otherCommits.removeAll(mr.getCommits());
					}else {
						LOG.info("MR "+ mr.getId() + " have merge commit and no other commits");
					}
					for(GitLabIssue issue : mr.getClosesIssues()) {
						ParsedIssue parsedIssue = createParsedIssues(issue);
						parsedIssue.addMergeRequests(Arrays.asList(parsedMR));
						parsedMR.getIssues().add(parsedIssue);
					}
					parsedRequests.add(parsedMR);
				}
			} catch (GitChangelogIntegrationException e1) {
				LOG.error("", e1);
			}
		}
		return parsedRequests;
	}

	public List<ParsedMergeRequest> getIssueMergeRequests(String issue, List<ParsedMergeRequest> mergedRequests ) {
		final GitLabClient gitLabClient = createGitLabClient();

		List<ParsedMergeRequest> parsedRequests = new ArrayList<>();
		List<GitLabMergeRequest> requests;
		if (gitLabClient != null ) {
			try {
				requests = gitLabClient.getIssueMergeRequests(settings.getGitLabProjectName().get(), issue);
				for(GitLabMergeRequest mr : requests) {
					Optional<ParsedMergeRequest> parsedMR = mergedRequests.stream().filter(e -> e.getId().equals(mr.getId())).findFirst();
					if(parsedMR.isPresent())
						parsedRequests.add(parsedMR.get());
					else
						LOG.info("Invalid MR: "+ mr.getId());
				}
			} catch (GitChangelogIntegrationException e1) {
				LOG.error("", e1);
			}
		}
		return parsedRequests;
	}


	private ParsedMergeRequest createParsedMergeRequest(GitLabMergeRequest mr) {
		if(mr.getId()== null)
			LOG.info("MR id is null");
		return new ParsedMergeRequest(mr.getId(),mr.getTitle(),  mr.getDescription(), mr.getLink(), mr.getLabels());
	}

	private ParsedIssue createParsedIssues(GitLabIssue issue) {
		if(!issues.containsKey(issue.getId()))
			issues.put(issue.getId(), new ParsedIssue(SettingsIssueType.GITLAB, "GitLab" ,issue.getId(), issue.getDesc(), issue.getLink(), issue.getTitle(), null, null, issue.getLabels()));
		return issues.get(issue.getId());
	}



	private GitLabClient createGitLabClient() {
		GitLabClient client = null;
		if (settings.getGitLabServer().isPresent()) {
			final String server = settings.getGitLabServer().get();
			final String token = settings.getGitLabToken().orNull();
			client = new GitLabClient(server, token);
		}
		return client;
	}

}
