package org.inspectit.wessbas.modules;

import java.util.ArrayList;
import java.util.List;

import org.inspectit.wessbas.sessionconversion.OpenXtraceSessionConverter;
import org.spec.research.open.xtrace.api.core.Trace;

/**
 * @author Alper Hidiroglu
 *
 */
public class OpenXtraceModule {

	private final String openXtraceLocation = "specifiy_location";

	public void execute() {

		List<Trace> openxtraces = new ArrayList<Trace>();
		OpenXtraceSessionConverter converter = new OpenXtraceSessionConverter();
		converter.convertOpenXtraceIntoSessionLog(openxtraces);
	}
}
