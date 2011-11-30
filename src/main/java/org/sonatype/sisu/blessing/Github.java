package org.sonatype.sisu.blessing;

import java.util.Collection;

import org.sonatype.sisu.blessing.dto.BuildResult;
import org.sonatype.sisu.blessing.dto.PullRequest;

public interface Github
{

    Collection<PullRequest> getPullRequests( final String owner, final String repository );

    void bless( PullRequest request, BuildResult result );

}
