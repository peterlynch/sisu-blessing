package org.sonatype.sisu.blessing.dto;

/**
 *
 */
public class PullRequest
{

    private final String repositoryUrl;

    private final String url;

    private final String repository;

    private final boolean open;

    private final int issueNumber;

    private final long timeStamp;

    private final String owner;

    public PullRequest( final String repositoryUrl, final String url, final String repository,
                        final boolean open, final int issueNumber, final long timeStamp, final String owner )
    {
        this.repositoryUrl = repositoryUrl;
        this.url = url;
        this.repository = repository;
        this.open = open;
        this.issueNumber = issueNumber;
        this.timeStamp = timeStamp;
        this.owner = owner;
    }

    public String getRepositoryUrl()
    {
        return repositoryUrl;
    }

    public String getUrl()
    {
        return url;
    }

    public String getRepository()
    {
        return repository;
    }

    public boolean isOpen()
    {
        return open;
    }

    public int getIssueNumber()
    {
        return issueNumber;
    }

    public long getTimeStamp()
    {
        return timeStamp;
    }

    public String getOwner()
    {
        return owner;
    }
}
