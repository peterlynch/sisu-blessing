package org.sonatype.sisu.blessing.commands;

import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.util.Arrays;
import javax.inject.Inject;
import javax.inject.Named;

import com.beust.jcommander.Parameter;
import com.google.common.base.Preconditions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.blessing.Github;
import org.sonatype.sisu.blessing.Hudson;
import org.sonatype.sisu.blessing.dto.PullRequest;

/**
 *
 */
@Named( "give" )
public class Bless
    extends Command
{

    private static final Logger log = LoggerFactory.getLogger( Bless.class );

    @Parameter( names = "-job", description = "Job name", required = true )
    private String jobName;

    @Parameter( names = "-nr", description = "Job number", required = true )
    private int buildNumber;

    private final Github github;

    private final Hudson hudson;

    @Inject
    public Bless( final Github github, final Hudson hudson )
    {
        this.github = Preconditions.checkNotNull( github );
        this.hudson = Preconditions.checkNotNull( hudson );
    }

    public void run()
    {
        try
        {
            PullRequest pullRequest = hudson.getPullRequest( jobName );
            github.bless( pullRequest, hudson.getResult( jobName, buildNumber ) );
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
