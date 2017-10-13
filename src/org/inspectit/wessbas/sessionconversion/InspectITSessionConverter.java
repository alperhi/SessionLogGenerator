package org.inspectit.wessbas.sessionconversion;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.apache.commons.io.FileUtils;

import rocks.inspectit.shared.all.communication.data.HttpTimerData;
import rocks.inspectit.shared.all.communication.data.InvocationSequenceData;

/**
 * Class for converting invocation sequences into session logs.
 *
 * @author Alper Hidiroglu, Jonas Kunz
 *
 */
public class InspectITSessionConverter {

	private final String FILEDIRECTORY = "C:/Users/ahi/Desktop/ContinuITy/generated-sessions/inspectit/sessions.dat";

	/**
	 * @param methods
	 * @param invocationSequences
	 */
	public void convertInvocationSequencesIntoSessionLog(Iterable<InvocationSequenceData> invocationSequences, HashMap<Long, String> businessTransactions) {

		HashMap<String, List<HttpTimerData>> sortedList = sortAfterSessionAndTimestamp(invocationSequences);

		writeIntoFile(sortedList, businessTransactions);

	}

	/**
	 * @param sortedList
	 * @param methods
	 */
	public void writeIntoFile(HashMap<String, List<HttpTimerData>> sortedList, HashMap<Long, String> businessTransactions) {
		boolean first = true;
		try {
			FileOutputStream fout = FileUtils.openOutputStream(new File(FILEDIRECTORY));
			PrintStream ps = new PrintStream(fout);
			for (List<HttpTimerData> currentList : sortedList.values()) {

				HttpTimerData firstElement = currentList.get(0);
				String sessionId = extractSessionIdFromCookies(firstElement);
				StringBuffer entry = new StringBuffer();
				entry.append(sessionId);

				for (HttpTimerData invoc : currentList) {
					// get http specific data
					// append(methods.get(invoc.getMethodIdent()).getMethodName()) wirft in seltenen
					// Fällen eine NullPointerException (warum?)
					// entry.append(";\"").append(methods.get(invoc.getMethodIdent()).getMethodName()).append("\":").append(invoc.getTimeStamp().getTime()
					// * 1000000).append(":")
					// .append((invoc.getTimeStamp().getTime() * 1000000) + ((long)
					// invoc.getDuration() * 1000000));

					if (businessTransactions.get(invoc.getId()) != null) {
						entry.append(";\"").append(businessTransactions.get(invoc.getId())).append("\":").append(invoc.getTimeStamp().getTime() * 1000000).append(":")
						.append((invoc.getTimeStamp().getTime() * 1000000) + ((long) invoc.getDuration() * 1000000));

						appendHTTPInfo(entry, invoc);
					}
				}
				if (first) {
					first = false;
				} else {
					ps.println();
				}
				ps.print(entry.toString());
			}
			ps.close();
			fout.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	/**
	 * @param invocationSequences
	 * @return
	 */
	public HashMap<String, List<HttpTimerData>> sortAfterSessionAndTimestamp(Iterable<InvocationSequenceData> invocationSequences) {

		HashMap<String, List<HttpTimerData>> sortedSessionsInvoc = new HashMap<String, List<HttpTimerData>>();

		ArrayList<HttpTimerData> sortedList = new ArrayList<HttpTimerData>();

		// Only InvocationSequenceData with SessionID != null
		for (InvocationSequenceData invoc : invocationSequences) {
			if ((invoc.getTimerData() != null) && (invoc.getTimerData() instanceof HttpTimerData)) {
				HttpTimerData dat = (HttpTimerData) invoc.getTimerData();
				if (extractSessionIdFromCookies(dat) != null) {
					sortedList.add(dat);
				}
			}
		}

		Collections.sort(sortedList, new Comparator<HttpTimerData>() {
			@Override
			public int compare(HttpTimerData data1, HttpTimerData data2) {
				return data1.getTimeStamp().getTime() > data2.getTimeStamp().getTime() ? 1 : (data1.getTimeStamp().getTime() < data2.getTimeStamp().getTime()) ? -1 : 0;
			}
		});

		for (HttpTimerData invoc : sortedList) {
			String sessionId = extractSessionIdFromCookies(invoc);
			if (sortedSessionsInvoc.containsKey(sessionId)) {
				sortedSessionsInvoc.get(sessionId).add(invoc);
			} else {
				List<HttpTimerData> newList = new ArrayList<HttpTimerData>();
				newList.add(invoc);
				sortedSessionsInvoc.put(sessionId, newList);
			}
		}
		return sortedSessionsInvoc;
	}

	/**
	 * @param entry
	 * @param seq
	 */
	private void appendHTTPInfo(StringBuffer entry, HttpTimerData dat) {
		try {

			String uri = dat.getHttpInfo().getUri();
			if (uri == null) {
				return;
			}

			String host_port = dat.getHeaders().get("host");
			String host = host_port;
			String port = "";
			if (host_port.contains(":")) {
				int i = host_port.indexOf(":");
				host = host_port.substring(0, i);
				port = host_port.substring(i + 1);
			}

			String protocol = "HTTP/1.1";
			String encoding = "<no-encoding>";
			String method = dat.getHttpInfo().getRequestMethod().toUpperCase();
			String queryString = "<no-query-string>";
			Map<String, String[]> params = Optional.ofNullable(dat.getParameters()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
			if (params != null) {
				queryString = encodeQueryString(params);
			}

			entry.append(":").append(uri);
			entry.append(":").append(port);
			entry.append(":").append(host);
			entry.append(":").append(protocol);
			entry.append(":").append(method);
			entry.append(":").append(queryString);
			entry.append(":").append(encoding);
		} catch (NoSuchElementException e) {
			return; // do nothing, some data is missing therefore omit writing
		}

	}

	/**
	 * @param dat
	 * @return
	 */
	private String extractSessionIdFromCookies(HttpTimerData dat) {
		String sessionID = null;
		String cookies = dat.getHeaders().get("cookie");
		if (cookies != null) {
			int begin = cookies.indexOf("JSESSIONID=");
			// Vorher wurde abgefragt ob != -1 (warum?)
			if (begin != -1) {
				begin += "JSESSIONID=".length();
				sessionID = "";
				while (begin < cookies.length()) {
					char c = cookies.charAt(begin);
					if (!Character.isLetterOrDigit(c)) {
						break;
					} else {
						sessionID += c;
						begin++;
					}
				}
			}
		}
		return sessionID;
	}

	/**
	 * @param params
	 * @return
	 */
	private String encodeQueryString(Map<String, String[]> params) {
		try {
			if (params.isEmpty()) {
				return "<no-query-string>";
			}
			StringBuffer result = new StringBuffer();
			for (String key : params.keySet()) {
				String encodedKey = URLEncoder.encode(key, "UTF-8");
				for (String value : params.get(key)) {
					String encodedValue = "";
					if (value != null) {
						encodedValue = "=" + URLEncoder.encode(value, "UTF-8");
					}

					if (result.length() > 0) {
						result.append("&");
					}
					result.append(encodedKey + encodedValue);

				}
			}
			return result.toString();
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}
}
