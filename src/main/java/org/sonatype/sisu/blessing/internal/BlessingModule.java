package org.sonatype.sisu.blessing.internal;

import javax.inject.Named;

import com.google.common.base.Throwables;
import com.google.inject.AbstractModule;
import org.sonatype.sisu.blessing.Github;
import org.sonatype.sisu.blessing.Hudson;

/**
 *
 */
@Named
public class BlessingModule
    extends AbstractModule
{

    @Override
    protected void configure()
    {
        try
        {
            bind( Hudson.class ).to( (Class<? extends Hudson>) Class.forName( System.getProperty( "hudson.impl", "org.sonatype.sisu.blessing.internal.MatrixImpl" ) ) );
        }
        catch ( ClassNotFoundException e )
        {
            Throwables.propagate( e );
        }
        bind( Github.class ).to( GithubImpl.class );
    }
}
