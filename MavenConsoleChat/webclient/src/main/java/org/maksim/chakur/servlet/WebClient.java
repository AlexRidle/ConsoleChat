package org.maksim.chakur.servlet;

import org.maksim.chakur.client.CheckNames;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@WebServlet("/WebClient")
public class WebClient extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private String name;
	private String register;
	private ClientService clientService;
	private static HashSet<String> names = new HashSet<>();

	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		response.setContentType("UTF-8");
		PrintWriter out = response.getWriter();
		String[] req = request.getParameterValues("comment");
		if (name != null && !name.equals("") && register != null) {
			StringBuffer stringBuffer = new StringBuffer();
			for (String str : req) {
				String subStr = str.replaceAll("\r", "");
				subStr = subStr.replaceAll("\n", " ");
				stringBuffer.append(subStr);
			}
			System.out.println(stringBuffer);
			clientService.sendOutputMessage(stringBuffer.toString());
			if (stringBuffer.toString().trim().contentEquals("/exit")) {
				synchronized(names) {
					names.remove(name);
				}
				name = null;
				register = null;
				out.println("<html>");
				out.println("<h3>You have been disconnected.</h3>");
				out.println("</html>");
			} else {
				// Вариант ожидания получения сообщений от сервера
				/*String message = "";
				while (true) {
					try {
						Thread.sleep(2000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
					message = clientService.getMessage();
					if(message != null && !message.equals("")) {
						break;
					}
				}*/
				String message = clientService.getMessage();
				out.println("<html>");
				out.println("<table border=\"1\" width=\"30%\" cellpadding=\"5\">");
				out.println("<tr><td>");
				out.println(message);
				out.println("</td></tr>");
				out.println("</table>");
				out.println("<form method=\"get\" action=\"WebClient\">");
				out.println("<p>Enter your message:<Br>");
				out.println("<textarea name=\"comment\" cols=\"40\" rows=\"4\"></textarea></p>");
				out.println("<p><input type=\"submit\" value=\"Send\">");
				out.println("<input type=\"reset\" value=\"Clear\"></p>");
				out.println("</html>");		
			}
			
		} else {
			out.println("<html>");
			out.println("<h3>Please, pass the registration for starting a chat.</h3>");
			out.println("<a href=http://localhost:8080/webclient/StartForm.html>form for login</a>");
			out.println("</html>");
		}
	}

	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		request.setCharacterEncoding("UTF-8");
		response.setContentType("UTF-8");
		PrintWriter out = response.getWriter();
		
		name = request.getParameter("firstname");
		register = request.getParameter("character");
		if (!name.equals("") && register != null) {
			synchronized(names) {
				if (names.contains(name) || CheckNames.addName(name)) {
					out.println("<html>");
					out.println("<h3>Such name is Existed, try again:</h3>");
					out.println("<a href=http://localhost:8080/webclient/StartForm.html>form for login</a>");
					out.println("</html>");
				} else {
					names.add(name);
					clientService = new ClientService(register, name);
					clientService.createConnection();
					// Вариант ожидания подключения соединения
					/*try {
						Thread.sleep(1000);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}*/
					String message = clientService.getMessage();
					if (message != null && !message.equals("")) {
						out.println("<html>");
						out.println("<b>" + message + "</b><Br>");
						out.println("<a href=http://localhost:8080/webclient/ChatFile.html>form for chat</a>");
						out.println("</html>");
					} else {
						names.remove(name);
						CheckNames.removeName(name); // Метод работает только при указании полного пути к файлу
						name = null;
						register = null;
						out.println("<html>");
						out.println("<h3>Something wrong on server, try again later</h3>");
						out.println("</html>");
					}
				}
			}
						
		} else {
			out.println("<html>");
			out.println("<h3>Not all parameters are filled, try again</h3>");
			out.println("<a href=http://localhost:8080/webclient/StartForm.html>form for login</a>");
			out.println("</html>");	
		}
	}
}
