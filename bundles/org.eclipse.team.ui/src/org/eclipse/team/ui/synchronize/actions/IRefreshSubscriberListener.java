package org.eclipse.team.ui.synchronize.actions;

public interface IRefreshSubscriberListener {	
	public void refreshStarted(IRefreshEvent event);	
	public void refreshDone(IRefreshEvent event);
}