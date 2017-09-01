package org.inspectit.wessbas.main;

import org.inspectit.wessbas.inspectitrest.RestInspectITModule;

/**
 * @author Alper Hidirogluz
 *
 */
public class Main {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		RestInspectITModule rest = new RestInspectITModule();
		rest.execute();

	}

}
