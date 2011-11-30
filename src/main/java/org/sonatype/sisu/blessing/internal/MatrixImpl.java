package org.sonatype.sisu.blessing.internal;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Named;

import com.beust.jcommander.internal.Lists;
import com.google.common.base.Function;
import com.google.common.base.Throwables;
import com.google.common.collect.Collections2;
import com.sonatype.matrix.rest.client.MatrixClient;
import com.sonatype.matrix.rest.client.OpenOptions;
import com.sonatype.matrix.rest.client.ext.BuildClient;
import com.sonatype.matrix.rest.client.ext.ProjectClient;
import com.sonatype.matrix.rest.model.build.BuildDTO;
import com.sonatype.matrix.rest.model.build.BuildEventDTO;
import com.sonatype.matrix.rest.model.build.BuildResultDTO;
import com.sonatype.matrix.rest.model.build.BuildStateDTO;
import com.sonatype.matrix.rest.model.project.ProjectDTO;
import org.ini4j.Ini;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.blessing.Hudson;
import org.sonatype.sisu.blessing.dto.BuildJob;
import org.sonatype.sisu.blessing.dto.BuildResult;
import org.sonatype.sisu.blessing.dto.PullRequest;

/**
 *
 */
@Named
public class MatrixImpl
    implements Hudson
{

    private static final Logger log = LoggerFactory.getLogger( MatrixImpl.class );

    private final MatrixClient hudson;

    private ProjectClient projects;

    private BuildClient builds;

    private final URI uri;

    private final OpenOptions options;

    @Inject
    public MatrixImpl( MatrixClient hudson )
        throws Exception
    {
        this.hudson = hudson;

        File config = new File( System.getProperty( "user.home" ), ".blessing" );
        if ( config.canRead() )
        {
            Ini ini = new Ini( config );
            Profile.Section hudsonCfg = ini.get( "hudson" );

            options = new OpenOptions();
            options.setUsername( hudsonCfg.get( "user" ) );
            options.setPassword( hudsonCfg.get( "password" ) );

            uri = URI.create( hudsonCfg.get( "url" ) );
        }
        else
        {
            throw new IllegalStateException( "Could not read configuration (~/.blessing)" );
        }
    }

    private void init()
    {
        hudson.open( uri, options );
        projects = hudson.ext( ProjectClient.class );
        builds = hudson.ext( BuildClient.class );
    }

    public void release()
        throws Exception
    {
        hudson.close();
        projects().close();
        builds().close();
    }

    private String name( PullRequest request )
    {
        return String.format( "blessing_%s_%s_%s", request.getOwner(), request.getRepository(), request.getIssueNumber() );
    }

    @Override
    public BuildJob createJob( final PullRequest request, final String templateJob )
    {
        String config = projects().getProjectConfig( templateJob );
        ProjectDTO dto = null;
        try
        {
            dto = projects().createProject( name( request ), new ByteArrayInputStream( config.getBytes( "UTF-8" ) ) );
        }
        catch ( UnsupportedEncodingException e )
        {
            // unlikely
            Throwables.propagate( e );
        }
        return new BuildJob( dto.getName(), dto.getUrl(), request );
    }

    private ProjectClient projects()
    {
        if ( projects == null )
        {
            init();
        }
        return projects;
    }

    @Override
    public BuildJob getJob( final PullRequest request )
    {
        try {
            ProjectDTO project = projects().getProject( name( request ) );
            return new BuildJob( project.getName(), project.getUrl(), request );
        } catch (Exception e) {
            return null;
        }
    }

    @Override
    public void removeJob( final PullRequest request )
    {
        final String name = name( request );

        List<BuildDTO> dtos = builds().getBuilds( name );
        for ( BuildDTO dto : dtos )
        {
            if ( !dto.getState().equals( BuildStateDTO.COMPLETED ) )
            {
                builds().stopBuild( name, dto.getNumber() );
                builds().addBuildListener( new BuildClient.BuildListener()
                {
                    @Override
                    public void buildStarted( final BuildEventDTO event )
                    {
                    }

                    @Override
                    public void buildStopped( final BuildEventDTO event )
                    {
                        builds().removeBuildListener( this );

                        // only running build stopped -> remove project
                        if ( event.getProjectName().equals( name ) )
                        {
                            projects().deleteProject( name );
                        }
                    }
                } );
            }
        }
    }

    private BuildClient builds()
    {
        if ( builds == null )
        {
            init();
        }
        return builds;
    }

    @Override
    public PullRequest getPullRequest( final String jobName )
    {
        ProjectDTO project = projects().getProject( jobName );
        return request( project.getName() );
    }

    private PullRequest request( final String name )
    {
        String[] split = name.split( "_" );
        return new PullRequest( null, null, split[1], true, Integer.valueOf( split[2] ), -1, split[0] );
    }

    @Override
    public BuildResult getResult( final String jobName, final int buildNumber )
    {
        BuildDTO dto = builds().getBuild( jobName, buildNumber );
        ProjectDTO project = projects().getProject( dto.getProjectName() );
        return new BuildResult( dto.getResult().equals( BuildResultDTO.SUCCESS ), dto.getUrl(),
                                new BuildJob( project.getName(), project.getUrl(), request( jobName ) ) );
    }

    @Override
    public Collection<BuildJob> syncJobs( final Collection<PullRequest> pullRequests, final String templateJob )
    {
        final List<BuildJob> createdJobs = Lists.newArrayList();

        for ( PullRequest pullRequest : pullRequests )
        {
            if ( pullRequest.isOpen() )
            {
                if ( getJob( pullRequest ) == null )
                {
                    // create
                    BuildJob job = createJob( pullRequest, templateJob );
                    startJob( job );
                    createdJobs.add( job );
                }
                else
                {
                    // run
                    List<BuildDTO> dtos = builds().getBuilds( name( pullRequest ) );
                    Iterator<BuildDTO> iterator = dtos.iterator();
                    if ( !iterator.hasNext() || ( iterator.next().getTimeStamp() <= pullRequest.getTimeStamp() ) )
                    {
                        startJob( getJob( pullRequest ) );
                    }
                }
            }
            else if ( !pullRequest.isOpen() && getJob( pullRequest ) != null )
            {
                // remove
                removeJob( pullRequest );
            }
        }

        return createdJobs;
    }

    @Override
    public void startJob( final BuildJob job )
    {
        // TODO direct URL for parameterized builds?
        projects().scheduleBuild( job.getName() );
    }
}
