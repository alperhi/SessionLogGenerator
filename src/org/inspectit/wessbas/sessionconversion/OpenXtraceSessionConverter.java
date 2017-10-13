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
import org.spec.research.open.xtrace.api.core.Trace;
import org.spec.research.open.xtrace.api.core.callables.HTTPRequestProcessing;

/**
 * Class for converting invocation sequences into session logs.
 *
 * @author Alper Hidiroglu, Jonas Kunz
 *
 */
public class OpenXtraceSessionConverter {

	private final String FILEDIRECTORY = "C:/Users/ahi/Desktop/ContinuITy/generated-sessions/openxtrace/sessions.dat";

	/**
	 * @param methods
	 * @param invocationSequences
	 */
	public void convertOpenXtraceIntoSessionLog(List<Trace> openXtraces) {

		HashMap<String, List<HTTPRequestProcessing>> sortedList = sortAfterSessionAndTimestamp(openXtraces);

		writeIntoFile(sortedList);

	}

	/**
	 * @param sortedList
	 * @param methods
	 */
	public void writeIntoFile(HashMap<String, List<HTTPRequestProcessing>> sortedList) {
		boolean first = true;
		try {
			FileOutputStream fout = FileUtils.openOutputStream(new File(FILEDIRECTORY));
			PrintStream ps = new PrintStream(fout);
			for (List<HTTPRequestProcessing> currentList : sortedList.values()) {

				HTTPRequestProcessing firstElement = currentList.get(0);
				String sessionId = extractSessionIdFromCookies(firstElement);
				StringBuffer entry = new StringBuffer();
				entry.append(sessionId);

				for (HTTPRequestProcessing httpRequest : currentList) {
					// get http specific data
					// append(methods.get(invoc.getMethodIdent()).getMethodName()) wirft in seltenen
					// Fällen eine NullPointerException (warum?)
					// entry.append(";\"").append(methods.get(invoc.getMethodIdent()).getMethodName()).append("\":").append(invoc.getTimeStamp().getTime()
					// * 1000000).append(":")
					// .append((invoc.getTimeStamp().getTime() * 1000000) + ((long)
					// invoc.getDuration() * 1000000));

					if (httpRequest.getContainingSubTrace().getLocation().getBusinessTransaction().get() != null) {
						entry.append(";\"").append(httpRequest.getContainingSubTrace().getLocation().getBusinessTransaction().get()).append("\":")
						.append(httpRequest.getTimestamp() * 1000000).append(":")
						.append((httpRequest.getTimestamp() * 1000000) + (httpRequest.getResponseTime() * 1000000));

						appendHTTPInfo(entry, httpRequest);
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
	public HashMap<String, List<HTTPRequestProcessing>> sortAfterSessionAndTimestamp(List<Trace> openXtraces) {

		HashMap<String, List<HTTPRequestProcessing>> sortedSessionsInvoc = new HashMap<String, List<HTTPRequestProcessing>>();

		ArrayList<HTTPRequestProcessing> sortedList = new ArrayList<HTTPRequestProcessing>();

		for (Trace trace : openXtraces) {
			if(trace.getRoot().getRoot() instanceof HTTPRequestProcessing){
				HTTPRequestProcessing httpRequest = (HTTPRequestProcessing) trace.getRoot().getRoot();
				if (extractSessionIdFromCookies(httpRequest) != null) {
					sortedList.add(httpRequest);
				}
			}
		}

		Collections.sort(sortedList, new Comparator<HTTPRequestProcessing>() {
			@Override
			public int compare(HTTPRequestProcessing data1, HTTPRequestProcessing data2) {
				return data1.getTimestamp() > data2.getTimestamp() ? 1 : (data1.getTimestamp() < data2.getTimestamp()) ? -1 : 0;
			}
		});

		for (HTTPRequestProcessing httpRequest : sortedList) {
			String sessionId = extractSessionIdFromCookies(httpRequest);
			if (sortedSessionsInvoc.containsKey(sessionId)) {
				sortedSessionsInvoc.get(sessionId).add(httpRequest);
			} else {
				List<HTTPRequestProcessing> newList = new ArrayList<HTTPRequestProcessing>();
				newList.add(httpRequest);
				sortedSessionsInvoc.put(sessionId, newList);
			}
		}
		return sortedSessionsInvoc;
	}

	/**
	 * @param entry
	 * @param seq
	 */
	private void appendHTTPInfo(StringBuffer entry, HTTPRequestProcessing httpRequest) {
		try {

			String uri = httpRequest.getUri();
			if (uri == null) {
				return;
			}

			String host_port = httpRequest.getHTTPHeaders().get().get("host");
			String host = host_port;
			String port = "";
			if (host_port.contains(":")) {
				int i = host_port.indexOf(":");
				host = host_port.substring(0, i);
				port = host_port.substring(i + 1);
			}

			String protocol = "HTTP/1.1";
			String encoding = "<no-encoding>";
			String method = httpRequest.getRequestMethod().get().toString().toUpperCase();
			String queryString = "<no-query-string>";
			Map<String, String[]> params = Optional.ofNullable(httpRequest.getHTTPParameters().get()).map(HashMap<String, String[]>::new).orElse(new HashMap<>());
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
	private String extractSessionIdFromCookies(HTTPRequestProcessing http) {
		String sessionID = null;
		String cookies = http.getHTTPHeaders().get().get("cookie");
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
