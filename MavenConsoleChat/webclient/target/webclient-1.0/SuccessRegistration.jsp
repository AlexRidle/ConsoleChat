<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Success user registration page</title>
</head>
<body>
	<jsp:useBean id="user" class="org.maksim.chakur.servlet.User" scope="application"/>
	
	 <div>
	 	<input name="usercharacter" type="text" hidden="true" value="<%= user.getCharacter() %>"/> <br>
	 	<input name="username" type="text" hidden="true" value="<%= user.getName() %>"/> <br>
        <input type="text" id="userinput" /> <br> 
        <input type="submit" value="Send Message to Server" onclick="start()" />
    </div>
    <div id="messages"></div>
    
	<script type="text/javascript">
        var webSocket = new WebSocket(
                'ws://localhost:8080/webclient/websocket');

        webSocket.onerror = function(event) {
            onError(event)
        };

        webSocket.onopen = function(event) {
            onOpen(event)
        };

        webSocket.onmessage = function(event) {
            onMessage(event)
        };

        function onMessage(event) {
        	var  context = document.getElementById('messages').innerHTML;
        	document.getElementById('messages').innerHTML = event.data + '</br>' + context;
        }

        function onOpen(event) {
            document.getElementById('messages').innerHTML = 'Now Connection established';
            var character = document.getElementsByName("usercharacter")[0].value;
            var name = document.getElementsByName("username")[0].value;
            webSocket.send("/register " + character + " " + name);
        }

        function onError(event) {
            alert(event.data);
        }

        function start() {
            var text = document.getElementById("userinput").value;
            webSocket.send(text);
            return false;
        }
    </script>
	
</body>
</html>