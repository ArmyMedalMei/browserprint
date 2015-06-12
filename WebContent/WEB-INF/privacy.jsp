<%@page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%><%-- These comments are to prevent excess whitespace in the output.
--%><%@page session="false"%><%--
--%><!DOCTYPE HTML PUBLIC "-//W3C//DTD HTML 4.01//EN" "http://www.w3.org/TR/html4/strict.dtd">
<html>
<head>
<meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
<title>Libre-Panopticlick</title>
<link type="text/css" href="style.css" rel="stylesheet">
</head>
<body>
<%@include file="header.html" %>
<div id="content">
	<h2>Information we collect</h2>
	<p>
		When a client submits themselves to fingerprinting we collect several pieces of data about them.
	</p>
	
	<h3>Fingerprint data</h3>
	<p>
		We collect the results of all the fingerprint tests.
		This includes:
	</p>
	<ul>
		<li>The User-Agent header of the HTTP request for the page.</li>
		<li>The Accept header, the Accept-Encoding header, and the Accept-Language header from the HTTP request for the page.</li>
		<li>The value of the DNT (Do Not Track) header from the HTTP request for the page.</li>
		<li>Whether the client is thought to be using Tor or not. Detected by performing a TorDNSEL request.</li>
		<li>Whether JavaScript is enabled or not.</li>
		<li>The name of the platform the browser is running on, detected using JavaScript and flash.</li>
		<li>A list of your installed browser plugins, as detected using JavaScript.</li>
		<li>The time-zone configured on your machine, as detected using JavaScript.</li>
		<li>The screen size and colour depth of the monitor displaying the client's web browser, as detected using JavaScript and flash.</li>
		<li>The language of the client's browser, as detected using Flash.</li>
		<li>The fonts installed on the client's machine, detected using Flash.</li>
		<li>Whether cookies are enabled, detected by creating a cookie and then retrieving it.</li>
		<li>Whether localStorage, sessionStorage, and Internet Explorer's userData are available in the browser, detected using JavaScript.</li>
		<li>The difference between the client and the server's clocks, in minutes, detected using JavaScript.</li>
		<li>The value returned by the JavaScript function toLocaleString() called on the UNIX epoch.</li>
		<li>The output of the JavaScript Math.tan(-1e300).</li>
		<li>An image created by the client's browser using a HTML5 canvas.</li>
		<li>A 3D image created by the client's browser using WebGL.</li>
		<li>The name of the WebGL vendor of the client's browser; this may be the name of the client's graphics card.</li>
		<li>The name of the WebGL renderer of the client's browser; this may be the name of the client's underlying graphics driver.</li>
	</ul>
	
	<h3>Cookies</h3>
	<p>
		This site makes use of cookies.
		One cookie is set to expire when the browser closes, this is used only to check whether cookies are enabled.
		Another cookie set to expire after 30 days, for the main purpose of preventing double counting of fingerprints.
	</p>
	
	<h3>IP addresses</h3>
	<p>
		The IP address of clients is stored along with their fingerprint.
	</p>
	
	<h3>Timestamps</h3>
	<p>
		The date and time that a fingerprint was taken is stored along with the fingerprint. 
	</p>
	
	<h2>Use of data</h2>
	<p>
		The purpose of this website is to compile a database of browser fingerprints from which statistics and other interesting data can be extracted.
		We will not track or attempt to track clients outside of this website.
	</p>
	
	<h3>Sharing of data</h3>
	<p>
		Fingerprints, aggregated data about fingerprints, and other statistics may be published and shared publicly.
		IP addresses will not be published, however statistics that can be derived from IP address, such as the country of clients may be published.
		Additionally incomplete IP addresses may be published.
		An effort will be made to protect the privacy of clients in situations where their privacy may be threatened.
	</p>
	<p>
		The database of collected data, may be shared with a third party upon request,
		 provided they have a valid reason (e.g. research) and they agree to abide by our privacy policy.
		We will do our best to prevent theft or misuse of collected data, however, such incidents are still possible.
	</p>
</div>
<%@include file="footer.jsp" %>
</body>
</html>