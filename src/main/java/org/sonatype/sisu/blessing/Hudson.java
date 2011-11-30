package org.sonatype.sisu.blessing;

import java.util.Collection;

import org.sonatype.sisu.blessing.dto.BuildJob;
import org.sonatype.sisu.blessing.dto.BuildResult;
import org.sonatype.sisu.blessing.dto.PullRequest;

public interface Hudson
{

    BuildJob createJob( PullRequest request, final String templateJob );

    BuildJob getJob( PullRequest request );

    void removeJob( PullRequest request );

    PullRequest getPullRequest( String jobName );

    BuildResult getResult( String jobName, final int buildNumber );

    /**
     * Create build jobs for new and remove jobs for closed pull requests.
     *
     * @return the created build jobs
     */
    Collection<BuildJob> syncJobs( final Collection<PullRequest> pullRequests, final String templateJob );

    void startJob( BuildJob job );

    void release()
        throws Exception;
}
