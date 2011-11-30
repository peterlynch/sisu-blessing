package org.sonatype.sisu.blessing.dto;

/**
 *
 */
public class BuildJob
{

    private final String name;

    private final String url;

    private final PullRequest reference;

    public BuildJob( final String name, final String url, final PullRequest reference )
    {
        this.name = name;
        this.url = url;
        this.reference = reference;
    }

    public String getName()
    {
        return name;
    }

    public String getUrl()
    {
        return url;
    }
}
