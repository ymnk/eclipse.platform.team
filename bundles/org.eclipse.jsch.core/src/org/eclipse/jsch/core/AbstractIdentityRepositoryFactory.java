package org.eclipse.jsch.core;

import com.jcraft.jsch.IdentityRepository;

public abstract class AbstractIdentityRepositoryFactory{
    public abstract IdentityRepository create();
}
