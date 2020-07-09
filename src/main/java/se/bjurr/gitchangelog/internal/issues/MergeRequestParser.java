package se.bjurr.gitchangelog.internal.issues;

import static org.slf4j.LoggerFactory.getLogger;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Optional;

import org.slf4j.Logger;
import se.bjurr.gitchangelog.api.exceptions.GitChangelogIntegrationException;
import se.bjurr.gitchangelog.internal.git.model.GitCommit;
import se.bjurr.gitchangelog.internal.integrations.gitlab.GitLabClient;
import se.bjurr.gitchangelog.internal.integrations.gitlab.GitLabMergeRequest;
import se.bjurr.gitchangelog.internal.model.ParsedMergeRequest;
import se.bjurr.gitchangelog.internal.settings.Settings;

public class MergeRequestParser {

	private static final Logger LOG = getLogger(MergeRequestParser.class);

	private final List<GitCommit> commits;
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

	public List<ParsedMergeRequest> getMergeRequests() {
		final GitLabClient gitLabClient = createGitLabClient();

		List<ParsedMergeRequest> parsedRequests = new ArrayList<>();
		Date fromDate = Collections.min(commits, Comparator.comparing(commit -> commit.getCommitTime())).getCommitTime();
		List<GitLabMergeRequest> requests;
		if (gitLabClient != null) {
			try {
				requests = gitLabClient.getAllMergeRequests(settings.getGitLabProjectName().get(), fromDate, settings.getForBranch().get());
				for (GitLabMergeRequest mr : requests) {
					ParsedMergeRequest parsedMR = createParsedMergeRequest(mr);
					Optional<GitCommit> commit = commits.stream().filter(e -> e.getHash().equals(mr.getMergeCommitSha())).findFirst();
					if (commit.isPresent())
						parsedMR.setGitCommit(commit.get());
					parsedRequests.add(parsedMR);
				}
			} catch (GitChangelogIntegrationException e1) {
				LOG.error("", e1);
			}
		}
		return parsedRequests;
	}

	public List<ParsedMergeRequest> getIssueMergeRequests(String issue) {
		final GitLabClient gitLabClient = createGitLabClient();

		List<ParsedMergeRequest> parsedRequests = new ArrayList<>();
		List<GitLabMergeRequest> requests;
		if (gitLabClient != null ) {
			try {
				requests = gitLabClient.getIssueMergeRequests(settings.getGitLabProjectName().get(), issue);
				for(GitLabMergeRequest mr : requests) {
					ParsedMergeRequest parsedMR = createParsedMergeRequest(mr);
					Optional<GitCommit> commit = commits.stream().filter(e -> e.getHash().equals(mr.getMergeCommitSha())).findFirst();
					if(commit.isPresent())
							parsedMR.setGitCommit(commit.get());
					parsedRequests.add(parsedMR);
				}
			} catch (GitChangelogIntegrationException e1) {
				LOG.error("", e1);
			}
		}
		return parsedRequests;
	}


	private ParsedMergeRequest createParsedMergeRequest(GitLabMergeRequest mr) {
		return new ParsedMergeRequest(mr.getTitle(), mr.getDescription(), mr.getLink(), mr.getLabels());
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
