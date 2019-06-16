package org.maksim.chakur.servlet;


import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/CheckUser")
public class CheckUser extends Dispatcher {
	private static final long serialVersionUID = 1L;

	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		ServletContext ctx = getServletContext();
		User user = UserList.findUser(request.getParameter("name"));
		if (user == null) {
			this.forward("/Registration.html", request, response);
		} else {
			if (!user.getPassword().equals(request.getParameter("password"))) {
				this.forward("/Registration.html", request, response);
			} else {
				ctx.setAttribute("user", user);
				this.forward("/SuccessRegistration.jsp", request, response);
			}
		}
	}

}
