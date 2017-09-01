package org.inspectit.wessbas.inspectitrest;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

import org.inspectit.wessbas.rest.InspectITRestClient;
import org.inspectit.wessbas.sessionconversion.SessionConverter;

import rocks.inspectit.shared.all.cmr.model.MethodIdent;
import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * Module for importing the Traces stored in the buffer of a CMR repository. Uses the version
 * independent REST-API of inspectIT. Converts traces into a Session Log readable by WESSBAS.
 *
 * @author Jonas Kunz, Alper Hidiroglu
 *
 */
public class RestInspectITModule {

	/**
	 * The location of the cmr to connect to.
	 */
	private final String CMRCONFIG = "localhost:8182";

	/**
	 * The name of the agent connect to the CMR whose data shall be imported.
	 */
	private String AGENTNAME = "dvdstore";


	/**
	 * Fetches invocation sequences and converts them into one session log.
	 */
	public void execute() {

		InspectITRestClient fetcher = new InspectITRestClient(CMRCONFIG);

		PlatformIdent agent;

		Map<Long, MethodIdent> methods = new HashMap<>();
		try {
			agent = StreamSupport.stream(fetcher.fetchAllAgents().spliterator(), false).filter((a) -> a.getAgentName().equalsIgnoreCase(AGENTNAME))
					.findFirst().get();

			for (MethodIdent method : fetcher.fetchAllMethods(agent.getId())) {
				methods.put(method.getId(), method);
			}
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Iterable<InvocationSequenceData> invocationSequences = fetcher.fetchAll(agent.getId());

		SessionConverter converter = new SessionConverter();

		converter.convertIntoSessionLog(methods, agent, invocationSequences);
	}

}
