package org.eclipse.team.internal.ui.jobs;


public interface IRefreshSubscriberListener {	
	public void refreshStarted(IRefreshEvent event);
	
	public void refreshDone(IRefreshEvent event);
}