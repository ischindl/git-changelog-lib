package se.bjurr.gitchangelog.internal.integrations.gitlab;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabCommit;
import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import se.bjurr.gitchangelog.api.exceptions.GitChangelogIntegrationException;
import se.bjurr.gitchangelog.internal.git.model.GitCommit;

public class GitLabClient {

	private static final Logger LOG = LoggerFactory.getLogger(GitLabClient.class);
	private static final String closesPattern = "Closes #(\\d*)";

	private static LoadingCache<GitLabProjectIssuesCacheKey, List<GitlabIssue>> cache =
		newBuilder()
			.maximumSize(10)
			.expireAfterWrite(1, MINUTES)
			.build(
				new CacheLoader<GitLabProjectIssuesCacheKey, List<GitlabIssue>>() {
					@Override
					public List<GitlabIssue> load(GitLabProjectIssuesCacheKey cacheKey)
							throws IOException {
						return getAllIssues(cacheKey);
					}
				});
	private final String hostUrl;
	private final String apiToken;

	public GitLabClient(String hostUrl, String apiToken) {
		this.hostUrl = hostUrl;
		this.apiToken = apiToken;
	}

	public Optional<GitLabIssue> getIssue(String projectName, Integer matchedIssue)
																					throws GitChangelogIntegrationException {
		GitlabAPI gitLabApi = GitlabAPI.connect(hostUrl, apiToken);
		GitlabProject project;
		try {
			project = gitLabApi.getProject(projectName);
		} catch (Exception e) {
			throw new GitChangelogIntegrationException(
				"Unable to find project \""
					+ projectName
					+ "\". It should be \"tomas.bjerre85/violations-test\" for a repo like: https://gitlab.com/tomas.bjerre85/violations-test",
														e);
		}
		Integer projectId = project.getId();
		String httpUrl = project.getWebUrl();
		try {
			List<GitlabIssue> issues =
				cache.get(new GitLabProjectIssuesCacheKey(hostUrl, apiToken, projectId));
			for (GitlabIssue candidate : issues) {
				if (candidate.getIid() == matchedIssue) {
					return Optional.of(createGitLabIssue(httpUrl, candidate));
				}
			}
			return Optional.absent();
		} catch (Exception e) {
			throw new GitChangelogIntegrationException(e.getMessage(), e);
		}
	}

	/**
	 * Return all Gitlab MergeRequest merged int projectName project after fromDate for targetBranch
	 *
	 * @param projectName
	 * @param fromDate
	 * @param targetBranch
	 * @return
	 * @throws GitChangelogIntegrationException
	 */
	public List<GitLabMergeRequest> getAllMergeRequests(String projectName, Date fromDate, String targetBranch) throws GitChangelogIntegrationException {
		List<GitLabMergeRequest> requests = new ArrayList<>();
		GitlabAPI gitLabApi = GitlabAPI.connect(hostUrl, apiToken);
		GitlabProject project;
		try {
			project = gitLabApi.getProject(projectName);
		} catch (Exception e) {
			throw new GitChangelogIntegrationException(
				"Unable to find project \""
					+ projectName
					+ "\". It should be \"tomas.bjerre85/violations-test\" for a repo like: https://gitlab.com/tomas.bjerre85/violations-test",
				e);
		}
		String fromdate = new SimpleDateFormat("yyyy-MM-dd").format(fromDate);
		if (targetBranch.contains("/")) {
			targetBranch = targetBranch.substring(targetBranch.lastIndexOf("/") + 1);
		}
		String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabMergeRequest.URL + "?updated_after=" + fromdate + "&target_branch=" + targetBranch;
		List<GitlabMergeRequest> mergeRequests = gitLabApi.retrieve().getAll(tailUrl, GitlabMergeRequest[].class);
		for (GitlabMergeRequest mr : mergeRequests) {
			GitLabMergeRequest gmr = createGitLabMergeRequest(project.getWebUrl(), mr);
			setMergeRequestCommits(gitLabApi, project, mr, gmr);
			setMergeRequestClosesIssues(gitLabApi, project, mr, gmr);
			requests.add(gmr);
		}
		return requests;
	}

	private void setMergeRequestClosesIssues(GitlabAPI gitLabApi, GitlabProject project, GitlabMergeRequest mr, GitLabMergeRequest gmr) {

		Pattern pattern = Pattern.compile(closesPattern);
		Matcher m = pattern.matcher(mr.getDescription());
		while (m.find()) {
			try {
				List<GitlabIssue> issues = cache.get(new GitLabProjectIssuesCacheKey(hostUrl, apiToken, project.getId()));
				java.util.Optional<GitlabIssue> issue = issues.stream().filter(e-> e.getIid() == Integer.parseInt(m.group(1))).findFirst();
				if (!issue.isPresent())
					LOG.info("Merge requests " + mr.getIid() + " no Closes issues");
				gmr.getClosesIssues().add(createGitLabIssue(project.getHttpUrl(), issue.get()));
			} catch (Exception e) {
				new GitChangelogIntegrationException("Merge requests have no Closes issues", e);
			}
		}

	}

	public void setMergeRequestCommits(GitlabAPI gitLabApi, GitlabProject project, GitlabMergeRequest mr, GitLabMergeRequest gmr) {
		String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabMergeRequest.URL + "/" + mr.getIid() + "/commits";
		try {
			List<GitlabCommit> commits = gitLabApi.retrieve().getAll(tailUrl, GitlabCommit[].class);
			if (commits.isEmpty() && mr.isMerged())
				throw new GitChangelogIntegrationException("Merge requests have no Commits");
			for (GitlabCommit cm : commits) {
				// create GitCommmit from GitlabCommitData
				gmr.getCommits().add(new GitCommit(cm.getAuthorName(), cm.getAuthorEmail(), cm.getCommittedDate(), cm.getMessage(), cm.getId(), true));
			}
		} catch (Exception e) {
			new GitChangelogIntegrationException("Merge requests have no Commits", e);
		}
	}

	public List<GitLabMergeRequest> getIssueMergeRequests(String projectName, String issue) throws GitChangelogIntegrationException {
		List<GitLabMergeRequest> requests = new ArrayList<>();
		GitlabAPI gitLabApi = GitlabAPI.connect(hostUrl, apiToken);
		GitlabProject project;
		try {
			project = gitLabApi.getProject(projectName);
		} catch (Exception e) {
			throw new GitChangelogIntegrationException(
				"Unable to find project \""
					+ projectName
					+ "\". It should be \"tomas.bjerre85/violations-test\" for a repo like: https://gitlab.com/tomas.bjerre85/violations-test",
				e);
		}
		String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabIssue.URL + "/" + issue + "/related_merge_requests";
		try {
			List<GitlabMergeRequest> mergeRequests = gitLabApi.retrieve().getAll(tailUrl, GitlabMergeRequest[].class);
			for (GitlabMergeRequest mr : mergeRequests) {
				requests.add(createGitLabMergeRequest(project.getWebUrl(), mr));
			}
		} catch (Exception e) {
			new GitChangelogIntegrationException("Issue no merge requests", e);
		}
		return requests;
	}

	/*  public Optional<GitLabMergeRequest> getMergeRequest(String projectName, GitCommit commit)
		      throws GitChangelogIntegrationException {
		    GitlabAPI gitLabApi = GitlabAPI.connect(hostUrl, apiToken);
		    GitlabProject project;
		    try {
		      project = gitLabApi.getProject(projectName);
		    } catch (Exception e) {
		      throw new GitChangelogIntegrationException(
		          "Unable to find project \""
		              + projectName
		              + "\". It should be \"tomas.bjerre85/violations-test\" for a repo like: https://gitlab.com/tomas.bjerre85/violations-test",
		          e);
		    }
		    Integer projectId = project.getId();
		    String httpUrl = project.getHttpUrl();
		    try {
		      List<GitlabMergeRequest> mergeRequest =
		          cacheMR.get(new GitLabProjectIssuesCacheKey(hostUrl, apiToken, projectId));
		      for (GitlabMergeRequest candidate : mergeRequest) {
		        if (candidate.getIid() == matchedMergeRequest) {
		          return Optional.of(createGitLabMergeRequest(httpUrl, candidate));
		        }
		      }
		      return Optional.absent();
		    } catch (Exception e) {
		      throw new GitChangelogIntegrationException(e.getMessage(), e);
		    }
		  }*/

	private GitLabIssue createGitLabIssue(String projectUrl, GitlabIssue candidate) {
		String title = candidate.getTitle();
		String desc = candidate.getDescription();
		String id = "" + candidate.getIid();
		String link = projectUrl + "/issues/" + candidate.getIid();
		List<String> labels = new ArrayList<>();
		for (String l : candidate.getLabels()) {
			labels.add(l);
		}
		return new GitLabIssue(id, title, desc, link, labels);
	}

	private GitLabMergeRequest createGitLabMergeRequest(String projectUrl, GitlabMergeRequest candidate) {
		String title = candidate.getTitle();
		if (candidate.getIid() == null)
			LOG.error("MR has null ID");
		String id = "" + candidate.getIid();
		String desc = candidate.getDescription();
		String link = projectUrl + "/merge_requests/" + candidate.getIid();
		String sha = candidate.getMergeCommitSHA();
		List<String> labels = new ArrayList<>();
		for (String l : candidate.getLabels()) {
			labels.add(l);
		}
		return new GitLabMergeRequest(id, title, desc, sha, link, labels);
	}

	private static List<GitlabIssue> getAllIssues(GitLabProjectIssuesCacheKey cacheKey)
																						throws IOException {
		String hostUrl = cacheKey.getHostUrl();
		String apiToken = cacheKey.getApiToken();
		GitlabAPI gitLabApi = GitlabAPI.connect(hostUrl, apiToken);
		GitlabProject project = new GitlabProject();
		project.setId(cacheKey.getProjectId());
		return gitLabApi.getIssues(project);
	}
}
