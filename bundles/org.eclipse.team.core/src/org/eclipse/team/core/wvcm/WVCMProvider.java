package org.eclipse.team.core.wvcm;

import javax.wvcm.Provider;
import javax.wvcm.ProviderFactory;
import javax.wvcm.Resource;

import org.eclipse.core.resources.IResource;
import org.eclipse.team.core.Team;

/**
 * 1. Do we need an authentication extension point or server?
 * 
 * 2. How many instances of a Provider should we need? If clients call ProviderFactory.create()
 * we can't control the instances created, or clients should have lightweight providers
 * with a related singleton backend.
 * 
 * 3. It has to be easy to find out if a deployment/repository provider has an API?
 * 
 * 4. Location string for local workspaces are of the form?
 * 
 * 5. ProviderFactory should still be useable within Eclipse.
 *
 * Examples:
 * ==> Getting access to the API  
 * DeploymentProvider dp = IDeploymentProvider.getMapping(project);
 * IWVCMProvider wvcm = (IWVCMProvider)dp.getAdapter(IWVCMProvider.class);
 * Resource r = wvcm.getProviderResource(dp.getContainer());
 * 
 * RepositoryProvider rp = RepositoryProvider.getProvider(project);
 * IWVCMProvider wvcm = (IWVCMProvider)rp.getAdapter(IWVCMProvider.class);
 * Resource r = wvcm.getProviderResource(rp.getProject());
 * 
 */
abstract public class WVCMProvider {
	abstract protected String getProviderName();
	
	/**
	 * Method used for:
	 * - clients wanting to perform simple team operations on resources in the
	 * workspace.
	 * @param resource
	 * @return
	 */
	public Resource getProviderResource(IResource resource) throws Exception {
		Provider provider = getProvider();
		return provider.location(resource.getLocation().toString()).resource();
	}
	
	/**
	 * Return an instance of the wvcm provider associated with this
	 * object.
	 */
	public Provider getProvider() throws Exception {
		return ProviderFactory.createProvider(getProviderName(), Team.getDefaultWVCMAuthentication().getDefaultAuthentication());
	}
}
