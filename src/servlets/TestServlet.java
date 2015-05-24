package servlets;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Date;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import util.TorCheck;
import DAOs.FingerprintDAO;
import beans.CharacteristicBean;
import beans.CharacteristicsBean;
import datastructures.Fingerprint;

/**
 * Servlet implementation class TestServlet
 */
public class TestServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public TestServlet() {
		super();
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		String js_enabled = request.getParameter("js_enabled");
		if (js_enabled == null) {
			/*
			 * The non-JS version of the page.
			 * Perform just a basic fingerprinting.
			 * None of the characteristics that require javascript.
			 */
			Fingerprint fingerprint = getBasicFingerprint(request);
			serveRequest(request, response, fingerprint);
			return;
		} else {
			/*
			 * The JS enabled version of the page.
			 * Do a full fingerprinting.
			 * Will perform javascript fingerprinting then submit fingerprint via a POST request.
			 */
			request.getRequestDispatcher("/WEB-INF/JsTest.jsp").forward(request, response);
			return;
		}
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse response)
	 */
	protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		Fingerprint fingerprint = getBasicFingerprint(request);

		/*
		 * Extract the rest of the fingerprint from the POST details.
		 */
		fingerprint.setPluginDetails(request.getParameter("PluginDetails"));
		fingerprint.setTimeZone(request.getParameter("TimeZone"));
		fingerprint.setScreenDetails(request.getParameter("ScreenDetails"));
		fingerprint.setFonts(request.getParameter("Fonts"));
		fingerprint.setSuperCookie(request.getParameter("SuperCookie"));
		fingerprint.setDateTime(request.getParameter("DateTime"));
		fingerprint.setMathTan(request.getParameter("MathTan"));
		{
			String adsBlocked = request.getParameter("AdsBlocked");
			if (adsBlocked != null) {
				if (adsBlocked.equals("1")) {
					fingerprint.setAdsBlocked(true);
				}
				else if(adsBlocked.equals("0")){
					fingerprint.setAdsBlocked(false);
				}
			}
		}
		fingerprint.setCanvas(request.getParameter("Canvas"));
		fingerprint.setWebGL(request.getParameter("WebGL"));
		fingerprint.setWebGLVendor(request.getParameter("WebGLVendor"));
		fingerprint.setWebGLRenderer(request.getParameter("WebGLRenderer"));

		{
			long ourTime = new Date().getTime();
			long theirTime;
			try {
				theirTime = Long.parseLong(request.getParameter("Time"));
			} catch (NumberFormatException ex) {
				// Difference of 0.
				theirTime = ourTime;
			}

			// Get how many minutes our times differ by.
			long difference = (ourTime - theirTime) / (1000 * 60);
			fingerprint.setClockDifference(difference);
		}

		serveRequest(request, response, fingerprint);
	}

	/**
	 * Finalise a request then forward it to the output page.
	 * 
	 * @param request
	 * @param response
	 * @param fingerprint
	 * @throws ServletException
	 * @throws IOException
	 */
	private void serveRequest(HttpServletRequest request, HttpServletResponse response, Fingerprint fingerprint) throws ServletException, IOException {
		CharacteristicsBean chrsbean = new CharacteristicsBean();
		CharacteristicBean uniquenessbean = new CharacteristicBean();
		FingerprintDAO.processFingerprint(fingerprint, chrsbean, uniquenessbean);
		request.setAttribute("chrBean", chrsbean);
		request.setAttribute("uniquessbean", uniquenessbean);

		/*
		 * Save SampleSetID in a cookie if we have one now.
		 */
		saveSampleSetID(response, fingerprint.getSampleSetID());

		/*
		 * Forward to the output page.
		 */
		request.getRequestDispatcher("/WEB-INF/Output.jsp").forward(request, response);
	}

	/**
	 * Get the basic fingerprint of a request.
	 * This consists of fingerprint properties that can be taken without JavaScript.
	 * 
	 * @param request
	 * @return
	 */
	private Fingerprint getBasicFingerprint(HttpServletRequest request) {
		Fingerprint fingerprint = new Fingerprint();

		fingerprint.setUser_agent(getUserAgentHeaderString(request));
		fingerprint.setAccept_headers(getAcceptHeadersString(request));
		fingerprint.setDoNotTrack(getDoNotTrackHeaderString(request));
		fingerprint.setUsingTor(TorCheck.isUsingTor(
				getServletContext().getInitParameter("serversPublicIP"),
				request.getLocalPort(),
				request.getRemoteAddr(),
				getServletContext().getInitParameter("TorDNSELServer")
				) == true);

		fingerprint.setIpAddress(getClientIP(request));

		Cookie cookies[] = request.getCookies();
		if (cookies != null) {
			fingerprint.setCookiesEnabled(true);
		}
		else {
			fingerprint.setCookiesEnabled(false);
		}
		fingerprint.setSampleSetID(getSampleSetID(request));

		return fingerprint;
	}

	/**
	 * Get the SampleSetID from a request.
	 * The browser uses this to prevent double counting of fingerprints.
	 * 
	 * @param request
	 * @return
	 */
	private Integer getSampleSetID(HttpServletRequest request) {
		Cookie cookies[] = request.getCookies();

		if (cookies == null) {
			// No SampleIDs. Just return an empty list.
			return null;
		}

		// Find the SampleIDs cookie.
		Integer sampleSetID = null;
		for (int i = 0; i < cookies.length; ++i) {
			if (cookies[i].getName().equals("SampleSetID")) {
				try {
					sampleSetID = Integer.parseInt(cookies[i].getValue());
					break;
				} catch (NumberFormatException ex) {
					// Ignore. Pretend invalid SampleSetID doesn't exist.
				}
			}
		}
		return sampleSetID;
	}

	/**
	 * Save a set of sample IDs to a cookie in the HTTP response.
	 * 
	 * @param response
	 * @param sampleIDs
	 */
	private void saveSampleSetID(HttpServletResponse response, Integer sampleSetID) {
		if (sampleSetID == null) {
			// This should never happen, but if it somehow did it could cause a null pointer exception.
			return;
		} else {
			Cookie sampleSetIdCookie = new Cookie("SampleSetID", sampleSetID.toString());
			sampleSetIdCookie.setMaxAge(60 * 60 * 24 * 30);// 30 days
			response.addCookie(sampleSetIdCookie);
		}
	}

	/**
	 * Get the User-Agent string of a request.
	 * 
	 * @param request
	 * @return
	 */
	private String getUserAgentHeaderString(HttpServletRequest request) {
		String useragent;
		try {
			// We get the header in this more long-winded way so that it may have unicode characters in it, such as Chinese.
			useragent = new String(request.getHeader("User-Agent").getBytes("ISO8859-1"), "UTF-8");
		} catch (Exception e) {
			// Fallback to regular method.
			useragent = request.getHeader("User-Agent");
		}
		return useragent;
	}

	/**
	 * Get the accept headers of a request.
	 * 
	 * @param request
	 * @return
	 */
	private String getAcceptHeadersString(HttpServletRequest request) {
		String accept = request.getHeader("accept");
		if (accept == null) {
			accept = "";
		}

		String accept_encoding = request.getHeader("accept-encoding");
		if (accept_encoding == null) {
			accept_encoding = "";
		}

		String accept_language = request.getHeader("accept-language");
		if (accept_language == null) {
			accept_language = "";
		}

		try {
			// We get the headers this more long-winded way so that they may have unicode characters inside them.
			return new String(accept.getBytes("ISO8859-1"), "UTF-8") + " "
					+ new String(accept_encoding.getBytes("ISO8859-1"), "UTF-8") + " "
					+ new String(accept_language.getBytes("ISO8859-1"), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// Fallback to regular method.
			return accept + " "
					+ accept_encoding + " "
					+ accept_language;
		}
	}

	/**
	 * Get the DNT header string of a request.
	 * 
	 * @param request
	 * @return
	 */
	private String getDoNotTrackHeaderString(HttpServletRequest request) {
		String dnt;
		try {
			// We get the header in this more long-winded way so that it may have unicode characters in it, such as Chinese.
			dnt = new String(request.getHeader("DNT").getBytes("ISO8859-1"), "UTF-8");
		} catch (Exception e) {
			// Fallback to regular method.
			dnt = request.getHeader("DNT");
		}
		return dnt;
	}

	/**
	 * Get the client's IP address in the format we want to save it.
	 * Format corresponds to IpAddressHandling context parameter in web.xml.
	 * FULL means save the full IP address.
	 * PARTIAL means zero out the last octet.
	 * Default is PARTIAL.
	 * 
	 * @param request
	 * @return
	 */
	private String getClientIP(HttpServletRequest request) {
		String ipHandling = getServletContext().getInitParameter("IpAddressHandling");
		if (ipHandling != null) {
			if (ipHandling.equals("FULL")) {
				// Collect full IP address.
				return request.getRemoteAddr();
			}
		}
		// Default handling method: Collect IP address with last octet set to zero
		String ip = request.getRemoteAddr();
		ip = ip.replaceAll("\\.\\d+$", ".0");
		return ip;
	}
}
