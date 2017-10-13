package org.inspectit.wessbas.modules;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.StreamSupport;

import org.inspectit.wessbas.rest.InspectITRestClient;
import org.inspectit.wessbas.sessionconversion.InspectITSessionConverter;

import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;

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

		try {
			agent = StreamSupport.stream(fetcher.fetchAllAgents().spliterator(), false).filter((a) -> a.getAgentName().equalsIgnoreCase(AGENTNAME))
					.findFirst().get();

		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		Iterable<ApplicationData> applications = null;
		List<Iterable<BusinessTransactionData>> allBusinessTransactionsOfAllApplications = new ArrayList<Iterable<BusinessTransactionData>>();

		try {
			applications = fetcher.fetchAllApplications();

			for (ApplicationData application : applications) {
				allBusinessTransactionsOfAllApplications.add(fetcher.fetchAllBusinessTransactions(application.getId()));
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		Iterable<BusinessTransactionData> justOneMonitoredApplication = allBusinessTransactionsOfAllApplications.get(0);

		HashMap<Integer, String> businessTransactionsMap = new HashMap<Integer, String>();

		for (BusinessTransactionData transaction : justOneMonitoredApplication) {
			int businessTransactionId = transaction.getId();
			String businessTransactionName = transaction.getName();
			businessTransactionsMap.put(businessTransactionId, businessTransactionName);
		}

		Iterable<InvocationSequenceData> invocationSequences = fetcher.fetchAll(agent.getId());

		HashMap<Long, String> businessTransactions = new HashMap<Long, String>();

		for (InvocationSequenceData invoc : invocationSequences) {
			if (businessTransactionsMap.get(invoc.getBusinessTransactionId()) != null) {
				String businessTransactionName = businessTransactionsMap.get(invoc.getBusinessTransactionId());

				if (!businessTransactionName.equals("Unknown Transaction")) {
					if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
						HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
						businessTransactions.put(dat.getId(), businessTransactionName);
					}
				}
			}
		}

		InspectITSessionConverter converter = new InspectITSessionConverter();

		converter.convertInvocationSequencesIntoSessionLog(invocationSequences, businessTransactions);


	}
}
