<!DOCTYPE html>
<%@ taglib prefix="spring" uri="http://www.springframework.org/tags"%>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<html lang="en">
    <head>
        <script>
            var connect = function () {
                var source = new EventSource('/register');

                // Handle correct opening of connection
                source.addEventListener('open', function (e) {
                    console.log('Connected.');
                });
                
                // Update the state when ever a message is sent
                source.addEventListener('message', function (e) {
                    var state = JSON.parse(e.data);
                    console.log("New state: " + state.text);
                    var element = document.getElementById("state");
                    element.innerHTML = state.text;
                }, false);

                // Reconnect if the connection fails
                source.addEventListener('error', function (e) {
                    console.log('Disconnected.');
                    if (e.readyState == EventSource.CLOSED) {
                        connected = false;
                        connect();
                    }
                }, false);
            };
        </script>
    </head>
    <body onload="connect();">
        <!-- Load the initial state from the application model. -->
        App status: <span id="state"><c:out value="${state.text}"/></span>
    </body>
</html>
