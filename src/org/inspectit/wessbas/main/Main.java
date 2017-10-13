package org.inspectit.wessbas.main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import org.inspectit.wessbas.modules.OpenXtraceModule;
import org.inspectit.wessbas.modules.RestInspectITModule;

/**
 * This Class provides the main method to start the Session Log Generator
 *
 * @author Alper Hidirogluz
 *
 */
public class Main {

	/**
	 * Main method to start the Session Log Generator. It can either convert traces in Open.xtrace
	 * format into a Session Log or invocation sequences into Session Logs.
	 *
	 * @param args
	 */
	public static void main(String[] args) {

		System.out.println("To generate Session Log from Open.xtrace trace, press 1. \nTo generate Session Log from Invocation Sequences, press 2.");
		BufferedReader in = new BufferedReader(new InputStreamReader(System.in));
		String input = "init";
		try {
			while ((!input.equals("1")) && (!input.equals("2"))) {
				input = in.readLine();
				if (!input.equals("1") && !input.equals("2")) {
					System.out.println("Please provide a valid input.");

				}
			}
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if (input.equals("1")) {
			OpenXtraceModule open = new OpenXtraceModule();
			open.execute();
		} else if (input.equals("2")) {
			RestInspectITModule rest = new RestInspectITModule();
			rest.execute();
		}

	}

}
