package org.eclipse.team.internal.uijobs;

import org.eclipse.core.runtime.jobs.ISchedulingRule;

public class BatchSimilarSchedulingRule implements ISchedulingRule {
	public String id;
	public BatchSimilarSchedulingRule(String id) {
		this.id = id;
	}		
	public boolean isConflicting(ISchedulingRule rule) {
		if(rule instanceof BatchSimilarSchedulingRule) {
			return ((BatchSimilarSchedulingRule)rule).id.equals(id);
		}
		return false;
	}
}