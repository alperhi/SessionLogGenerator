package org.inspectit.wessbas.rest;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import rocks.inspectit.shared.all.cmr.model.MethodIdent;
import rocks.inspectit.shared.all.cmr.model.PlatformIdent;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;
import rocks.inspectit.shared.all.communication.data.cmr.ApplicationData;
import rocks.inspectit.shared.all.communication.data.cmr.BusinessTransactionData;


/**
 *
 * Class for managing the REST connection with an inpsectIT CMR.
 *
 * @author Jonas Kunz, Alper Hidiroglu
 *
 */
public class InspectITRestClient {

	private static final String PARAM_PLATFOMR_ID = "platformId";


	private static final String ALL_AGENTS_PATH = "/rest/data/agents";

	private static final String ALL_METHODS_PATH = "/rest/data/agents";

	private static final String ALL_APPLICATIONS_PATH = "/rest/bc/app";

	private JsonHTTPClientWrapper rest;

	/**
	 * @param hostWithPort host:port of the CMR to connect to
	 */
	public InspectITRestClient(String hostWithPort) {
		super();
		rest = new JsonHTTPClientWrapper(hostWithPort);
	}

	/**
	 * Fetches all Invocation Sequences from the CMRs buffer for the given agent.
	 *
	 * @param platformID the id of the agent
	 * @return
	 */
	public Iterable<InvocationSequenceData> fetchAll(long platformID){
		final Map<String,Object> filterParams = new HashMap<>();
		filterParams.put(PARAM_PLATFOMR_ID, platformID);

		return new Iterable<InvocationSequenceData>() {

			@Override
			public Iterator<InvocationSequenceData> iterator() {
				return new RESTInvocationSequencesIterator(rest,filterParams);
			}
		};
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<ApplicationData> fetchAllApplications() throws IOException {

		ApplicationData[] result = rest.performGet(ALL_APPLICATIONS_PATH, ApplicationData[].class);

		return Arrays.asList(result);
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<BusinessTransactionData> fetchAllBusinessTransactions(int appId) throws IOException {

		BusinessTransactionData[] result = rest.performGet(ALL_APPLICATIONS_PATH + "/" + appId + "/btx", BusinessTransactionData[].class);

		return Arrays.asList(result);
	}

	/**
	 * @return A list of all Agents known to the CMR
	 * @throws IOException
	 */
	public Iterable<PlatformIdent> fetchAllAgents() throws IOException {

		PlatformIdent[] result = rest.performGet(ALL_AGENTS_PATH, PlatformIdent[].class);

		return Arrays.asList(result);
	}


	/**
	 * Fetches all Method Identifiers of instrumented Methods for a given Agent.
	 *
	 * @param platformId the agent to fetch from
	 * @return all MethodIdents
	 * @throws IOException
	 */
	public Iterable<MethodIdent> fetchAllMethods(long platformId) throws IOException {

		Map<String,String> params = new HashMap<>();
		params.put(PARAM_PLATFOMR_ID, ""+platformId);
		MethodIdent[] result = rest.performGet(ALL_METHODS_PATH + "/" + platformId + "/methods", MethodIdent[].class, params);

		return Arrays.asList(result);
	}

	/**
	 * Closes the connection.
	 */
	public void close() {
		rest.destroy();
	}

}
