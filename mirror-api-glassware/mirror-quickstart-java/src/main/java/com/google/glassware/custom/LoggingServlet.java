package com.google.glassware.custom;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class LoggingServlet
 */
public class LoggingServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public LoggingServlet() {
		super();
		System.out.println("----------------------------Inited-----------------------");
		ClassLoader cl = LoggingServlet.class.getClassLoader();
		InputStream inputStream = null;
		if (cl != null) {
			inputStream = cl.getResourceAsStream("log.properties");
		} else {
			inputStream = ClassLoader.getSystemResourceAsStream("log.properties");
		}
		java.util.logging.LogManager logManager = java.util.logging.LogManager.getLogManager();
		try {
			logManager.readConfiguration(inputStream);
		} catch (SecurityException e) {
			System.err.println(e);
		} catch (IOException e) {
			System.err.println(e);
		}
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// TODO Auto-generated method stub
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException,
			IOException {
		// TODO Auto-generated method stub
	}

}
