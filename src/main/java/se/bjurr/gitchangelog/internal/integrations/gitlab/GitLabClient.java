package se.bjurr.gitchangelog.internal.integrations.gitlab;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.MINUTES;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.google.common.base.Optional;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import org.gitlab.api.GitlabAPI;
import org.gitlab.api.models.GitlabIssue;
import org.gitlab.api.models.GitlabMergeRequest;
import org.gitlab.api.models.GitlabProject;
import se.bjurr.gitchangelog.api.exceptions.GitChangelogIntegrationException;

public class GitLabClient {

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

	public List<GitLabMergeRequest> getAllMergeRequests(String projectName, Date fromDate, String targetBranch) throws GitChangelogIntegrationException{
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
	    if(targetBranch.contains("/")) {
	    	targetBranch = targetBranch.substring(targetBranch.lastIndexOf("/")+1);
	    }
	    String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabMergeRequest.URL+"?updated_after="+fromdate+"&target_branch="+targetBranch;
        List<GitlabMergeRequest> mergeRequests = gitLabApi.retrieve().getAll(tailUrl, GitlabMergeRequest[].class);
        for (GitlabMergeRequest mr : mergeRequests) {
        	requests.add(createGitLabMergeRequest(project.getWebUrl(),mr));
        }
        return requests;
	}

	public List<GitLabMergeRequest> getIssueMergeRequests(String projectName, String issue) throws GitChangelogIntegrationException{
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
	    String tailUrl = GitlabProject.URL + "/" + project.getId() + GitlabIssue.URL+"/"+issue+"/related_merge_requests";
	    try {
	    	List<GitlabMergeRequest> mergeRequests = gitLabApi.retrieve().getAll(tailUrl, GitlabMergeRequest[].class);
	    	for (GitlabMergeRequest mr : mergeRequests) {
	    		requests.add(createGitLabMergeRequest(project.getWebUrl(),mr));
	    	}
	    }catch(Exception e) {
	    	new GitChangelogIntegrationException("Issue mo merge requests",e);
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
    String link = projectUrl + "/issues/" + candidate.getIid();
    List<String> labels = new ArrayList<>();
    for (String l : candidate.getLabels()) {
      labels.add(l);
    }
    return new GitLabIssue(title, desc, link, labels);
  }

  private GitLabMergeRequest createGitLabMergeRequest(String projectUrl, GitlabMergeRequest candidate) {
	    String title = candidate.getTitle();
	    String desc = candidate.getDescription();
	    String link = projectUrl + "/merge_requests/" + candidate.getIid();
	    String sha = candidate.getMergeCommitSHA();
	    List<String> labels = new ArrayList<>();
	    for (String l : candidate.getLabels()) {
	      labels.add(l);
	    }
	    return new GitLabMergeRequest(sha, title, desc, link, labels);
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
