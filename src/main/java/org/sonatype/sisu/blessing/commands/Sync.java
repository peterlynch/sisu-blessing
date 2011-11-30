package org.sonatype.sisu.blessing.commands;

import java.util.Collection;
import javax.inject.Inject;
import javax.inject.Named;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.blessing.Github;
import org.sonatype.sisu.blessing.Hudson;
import org.sonatype.sisu.blessing.dto.BuildJob;
import org.sonatype.sisu.blessing.dto.PullRequest;
import org.sonatype.sisu.blessing.internal.MatrixImpl;

/**
 *
 */
@Named( "sync" )
public class Sync
    extends Command
{

    private static final Logger log = LoggerFactory.getLogger( Sync.class );

    @Parameter( names = "-owner", description = "Name of repository owner", required = true )
    private String owner;

    @Parameter( names = "-repository", description = "Name of repository", required = true )
    private String repository;

    @Parameter( names = "-job", description = "Name of job template", required = true )
    private String template;

    private final Github github;

    private final Hudson hudson;

    @Inject
    public Sync( final Github github, final Hudson hudson )
    {
        this.github = Preconditions.checkNotNull( github );
        this.hudson = Preconditions.checkNotNull( hudson );
    }

    public void run()
    {
        try
        {
            Collection<PullRequest> pullRequests = github.getPullRequests( owner, repository );
            Collection<BuildJob> buildJobs = hudson.syncJobs( pullRequests, template );
            for ( BuildJob buildJob : buildJobs )
            {

            }
        }
        catch (Exception e )
        {
            log.error( e.getMessage(), e );
        }
        finally
        {
            try
            {
                hudson.release();
            }
            catch ( Exception e )
            {
                log.error( "Could not close hudson REST client properly", e );
            }

        }

        // forcefully exit, we get dangling connections with hudson client
        // the BuildClientImpl does not call super.close(), that does not kill connections (?)
        System.exit( 0 );

    }
}
