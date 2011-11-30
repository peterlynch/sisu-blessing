package org.sonatype.sisu.blessing.internal;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Set;
import javax.inject.Inject;
import javax.inject.Named;

import com.beust.jcommander.internal.Lists;
import com.github.api.v2.schema.Commit;
import com.github.api.v2.schema.Discussion;
import com.github.api.v2.schema.Issue;
import com.github.api.v2.schema.Repository;
import com.github.api.v2.services.CommitService;
import com.github.api.v2.services.GitHubServiceFactory;
import com.github.api.v2.services.IssueService;
import com.github.api.v2.services.PullRequestService;
import com.github.api.v2.services.auth.LoginTokenAuthentication;
import com.google.common.base.Function;
import com.google.common.collect.Collections2;
import com.google.common.collect.Sets;
import org.ini4j.Ini;
import org.ini4j.IniPreferencesFactory;
import org.ini4j.Profile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonatype.sisu.blessing.Github;
import org.sonatype.sisu.blessing.dto.BuildResult;
import org.sonatype.sisu.blessing.dto.PullRequest;

/**
 *
 */
@Named
public class GithubImpl
    implements Github
{

    private static final Logger log = LoggerFactory.getLogger( GithubImpl.class );

    private GitHubServiceFactory factory = GitHubServiceFactory.newInstance();

    private PullRequestService pullRequests = factory.createPullRequestService();

    private IssueService issues = factory.createIssueService();

    private CommitService commits = factory.createCommitService();

    @Inject
    public GithubImpl()
    {
        try
        {
            File gitconfig = new File( System.getProperty( "user.home" ), ".blessing" );
            if ( gitconfig.canRead() )
            {
                Ini ini = new Ini( gitconfig );
                Profile.Section github = ini.get( "github" );

                String user = github.get( "user" );
                String token = github.get( "token" );
                LoginTokenAuthentication authentication = new LoginTokenAuthentication( user, token );

                issues.setAuthentication( authentication );
                pullRequests.setAuthentication( authentication );
                commits.setAuthentication( authentication );
            }
        }
        catch ( IOException e )
        {
            log.warn( "Could not parse ~/.gitconfig file" );
        }

    }

    @Override
    public void bless( final PullRequest request, final BuildResult result )
    {
        issues.addComment( request.getOwner(), request.getRepository(), request.getIssueNumber(), result.toString() );
    }

    @Override
    public Collection<PullRequest> getPullRequests( final String owner, final String repository )
    {
        final List<PullRequest> dtos = Lists.newArrayList();

        // need to resolve discussions to get real updated time
        Collection<com.github.api.v2.schema.PullRequest> requests =
            Collections2.transform( pullRequests.getPullRequests( owner, repository ),
                                    new Function<com.github.api.v2.schema.PullRequest, com.github.api.v2.schema.PullRequest>()
                                    {
                                        @Override
                                        public com.github.api.v2.schema.PullRequest apply(
                                            final com.github.api.v2.schema.PullRequest input )
                                        {
                                            return pullRequests.getPullRequest( owner, repository, input.getNumber() );
                                        }
                                    } );

        for ( com.github.api.v2.schema.PullRequest request : requests )
        {
            Repository repo = request.getBase().getRepository();

            long timestamp = 0;
            List<Discussion> list = request.getDiscussion();
            Collections.reverse( list );
            for ( Discussion discussion : list )
            {
                if ( Discussion.Type.COMMIT.equals( discussion.getType() ) )
                {
                    timestamp = discussion.getCommittedDate().getTime();
                    break;
                }
            }

            dtos.add(
                new PullRequest(
                    repo.getUrl(),
                    request.getHtmlUrl(),
                    repo.getName(),
                    request.getState().equals( Issue.State.OPEN ),
                    request.getNumber(),
                    timestamp,
                    request.getBase().getUser().getLogin()
                )
            );
        }

        log.debug( dtos.toString() );

        return dtos;
    }

}
