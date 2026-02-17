<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<%
    if (session.getAttribute("user") != null) {
        response.sendRedirect("pages/dashboard.html");
    } else {
        response.sendRedirect("pages/login.html");
    }
%>
<!DOCTYPE html>
<html>
<head>
    <title>Redirecting...</title>
</head>
<body>
    <p>Redirecting to <a href="pages/login.html">login.html</a>...</p>
</body>
</html>
