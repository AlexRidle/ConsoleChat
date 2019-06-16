package org.maksim.chakur.servlet;

import java.io.IOException;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/AddUser")
public class AddUser extends Dispatcher {
	private static final long serialVersionUID = 1L;

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		ServletContext ctx = getServletContext();
		
		if (request.getParameter("save") != null) {
			String name = request.getParameter("name");
            String password = request.getParameter("password");
            String character = request.getParameter("character");
            
            User newUser = new User(name, password, character);

            ctx.setAttribute("user", newUser);

            boolean res = UserList.addUser(newUser);
            
            if (res) {
            	this.forward("/SuccessRegistration.jsp", request, response);
            } else {
            	this.forward("/ErrorRegistration.html", request, response);
            }
		} else if (request.getParameter("cancel") != null) {
			this.forward("/Login.html", request, response);
		}
	}

}
