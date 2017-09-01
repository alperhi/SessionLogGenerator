package org.inspectit.wessbas.fs;


import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

public class InvocationSequences implements Iterable<InvocationSequenceData> {
	private final List<InvocationSequenceData> invocationSequences;
	private PlatformIdent platformIdent;

	public InvocationSequences() {
		invocationSequences = new LinkedList<InvocationSequenceData>();
	}

	/**
	 * @return the platformIdent
	 */
	public PlatformIdent getPlatformIdent() {
		return platformIdent;
	}

	/**
	 * @param platformIdent
	 *            the platformIdent to set
	 */
	public void setPlatformIdent(PlatformIdent platformIdent) {
		this.platformIdent = platformIdent;
	}

	/**
	 * @return the invocationSequences
	 */
	public List<InvocationSequenceData> getInvocationSequences() {
		return invocationSequences;
	}

	public void addInvocationSequence(InvocationSequenceData isData) {
		invocationSequences.add(isData);
	}


	@Override
	public Iterator<InvocationSequenceData> iterator() {
		return invocationSequences.iterator();
	}

}
