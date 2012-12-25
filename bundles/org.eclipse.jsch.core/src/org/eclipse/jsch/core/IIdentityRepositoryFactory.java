package org.eclipse.jsch.core;

import com.jcraft.jsch.IdentityRepository;

public interface IIdentityRepositoryFactory{
    IdentityRepository create();
}
