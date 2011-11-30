package org.sonatype.sisu.blessing.dto;

/**
 *
 */
public class BuildResult
{

    private final boolean successful;

    private final String url;

    private final BuildJob job;

    public BuildResult( final boolean successful, final String url, final BuildJob job )
    {
        this.successful = successful;
        this.url = url;
        this.job = job;
    }

    public boolean isSuccessful()
    {
        return successful;
    }

    public String getUrl()
    {
        return url;
    }

    public BuildJob getJob()
    {
        return job;
    }

    @Override
    public String toString()
    {
        return String.format( "%s\n%s", successful ? "+1" : "-1", url );
    }
}
