package org.maksim.chakur.servlet;


import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/UserRegistration")
public class UserRegistration extends Dispatcher {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		// ServletContext ctx = getServletContext();
		if (request.getParameter("login") != null) {
			 this.forward("/CheckUser", request, response);
		} else if (request.getParameter("registration") != null) {
			this.forward("/Registration.html", request, response);
		}	
	}

}
