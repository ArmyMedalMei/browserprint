package DAOs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

import org.apache.commons.lang3.StringEscapeUtils;

import beans.CharacteristicBean;
import beans.CharacteristicsBean;
import datastructures.Fingerprint;

public class FingerprintDAO {
	/**
	 * Get the threadID of a post.
	 * 
	 * @param board
	 * @param postID
	 * @return the threadID of the post if successful. Else returns null if the
	 *         post doesn't exist or an error occurs.
	 */
	private static final String insertSampleStr = "INSERT INTO `Samples`(`IP`,`TimeStamp`,`UserAgent`, `AcceptHeaders`, `PluginDetails`, `TimeZone`, `ScreenDetails`, `Fonts`, `CookiesEnabled`, `SuperCookie`, `DoNotTrack`, `ClockDifference`, `DateTime`, `MathTan`, `UsingTor`, `AdsBlocked`, `Canvas`, `WebGL`, `WebGLVendor`, `WebGLRenderer`) VALUES(?, NOW(), ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?);";
	private static final String getSampleCountStr = "SELECT COUNT(*) FROM `Samples`;";

	private static final String NO_JAVASCRIPT = "no javascript";

	public static final Integer processFingerprint(Fingerprint fingerprint, CharacteristicsBean chrsbean, CharacteristicBean uniquenessbean) {
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(false);

			/*
			 * Check if we've seen this sample before.
			 */
			Integer sampleID = checkSampleChanged(conn, fingerprint);

			if (sampleID == null) {
				/*
				 * We haven't seen this sample before.
				 * Record it.
				 */
				sampleID = insertSample(conn, fingerprint);
				
				/*
				 * Insert SampleID into SampleSets table.
				 */
				insertSampleSet(conn, fingerprint, sampleID);
			}

			/*
			 * Get number of samples.
			 */
			int sampleCount = getSampleCount(conn);

			/*
			 * Get uniqueness.
			 */
			int sampleOccurrences = getSampleOccurrences(conn, fingerprint);
			uniquenessbean.setName("");
			if (sampleOccurrences == 1) {
				uniquenessbean.setValue("unique");
			} else {
				uniquenessbean.setValue("");
			}
			uniquenessbean.setInX(((double) sampleCount) / ((double) sampleOccurrences));
			uniquenessbean.setBits(Math.abs(Math.log(uniquenessbean.getInX()) / Math.log(2)));

			/*
			 * Get each characteristic.
			 */
			ArrayList<CharacteristicBean> characteristics = chrsbean.getCharacteristics();
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "UserAgent", fingerprint.getUser_agent());
				bean.setName("User Agent");
				bean.setNameHoverText("The User-Agent header sent with the HTTP request for the page.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "AcceptHeaders", fingerprint.getAccept_headers());
				bean.setName("HTTP_ACCEPT Headers");
				bean.setNameHoverText("The concatenation of three headers from the HTTP request:"
						+ " The Accept request header, the Accept-Encoding request header, and the Accept-Language request header.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "PluginDetails", fingerprint.getPluginDetails());
				bean.setName("Browser Plugin Details");
				bean.setNameHoverText("A list of the browsers installed plugins as detected using JavaScript.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "TimeZone", fingerprint.getTimeZone());
				bean.setName("Time Zone");
				bean.setNameHoverText("The time-zone configured on the client's machine.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "ScreenDetails", fingerprint.getScreenDetails());
				bean.setName("Screen Size and Color Depth");
				bean.setNameHoverText("The screen size and colour depth of the monitor displaying the client's web browser.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "Fonts", fingerprint.getFonts());
				if (bean.getValue().equals("")) {
					bean.setValue("No fonts detected");
				}
				bean.setName("System Fonts");
				bean.setNameHoverText("The fonts installed on the client's machine, detected using Flash.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "CookiesEnabled", fingerprint.isCookiesEnabled());
				bean.setName("Are Cookies Enabled?");
				bean.setNameHoverText("Whether cookies are enabled.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "SuperCookie", fingerprint.getSuperCookie());
				bean.setName("Limited supercookie test");
				bean.setNameHoverText("Three tests of whether DOM storage is supported (and enabled) in the client's web browser."
						+ " Tests for localStorage, sessionStorage, and Internet Explorer's userData.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "DoNotTrack", fingerprint.getDoNotTrack());
				if (bean.getValue().equals(NO_JAVASCRIPT)) {
					bean.setValue("No preference");
				}
				bean.setName("Do Not Track header");
				bean.setNameHoverText("The value of the DNT (Do Not Track) header from the HTTP request.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "ClockDifference", fingerprint.getClockDifference());
				bean.setName("Client/server time difference (minutes)");
				bean.setNameHoverText("The approximate amount of difference between the time on the client's computer and the clock on the server."
						+ " i.e., the clock on the client's computer is 5 minutes ahead of the clock on the server.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "DateTime", fingerprint.getDateTime());
				bean.setName("Date/Time format");
				bean.setNameHoverText("When the JavaScript function toLocaleString() is called on a date it can reveal information about the language of the browser via the names of days and months."
						+ " For instance the output 'Thursday January 01, 10:30:00 GMT+1030 1970' reveals that English is our configured language because 'Thursday' is English."
						+ " Additionally different browsers tend to return differently formatted results."
						+ " For instance Opera returns the above whereas Firefox returns '1/1/1970 9:30:00 am' for the same date (UNIX epoch)."
						+ " Additionally timezone information may be revealed."
						+ " For instance the above were taken on a computer configured for CST (+9:30), which is why the times shown aren't midnight.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "MathTan", fingerprint.getMathTan());
				bean.setName("Math/Tan function");
				bean.setNameHoverText("The same math functions run on different platforms and browsers can produce different results."
						+ " In particular we are interested in the output of Math.tan(-1e300), which has been observed to produce different values depending on operating system."
						+ " For instance on a 64bit Linux machine it produces the value -1.4214488238747245 and on a Windows machine it produces the value -4.987183803371025.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "UsingTor", fingerprint.isUsingTor());
				bean.setName("Using Tor?");
				bean.setNameHoverText("Checks whether a client's request came from a Tor exit node, and hence whether they're using Tor."
						+ " It does so by performing a TorDNSEL request for each client.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "AdsBlocked", fingerprint.getAdsBlocked());
				bean.setName("Ads blocked?");
				bean.setNameHoverText("Checks whether ad blocking software is installed."
						+ " It does so by attempting to display an ad and checking whether it was successful.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "Canvas", fingerprint.getCanvas());
				bean.setName("Canvas");
				if(bean.getValue().equals(NO_JAVASCRIPT) == false){
					bean.setValue("<img width=\"400\" height=\"60\" src=\"" + bean.getValue() + "\">");
				}
				bean.setNameHoverText("Rendering of a specific picture with the HTML5 Canvas element following a fixed set of instructions."
						+ " The picture presents some slight noticeable variations depending on the OS and the browser used.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "WebGL", fingerprint.getWebGL());
				bean.setName("WebGL");
				if(bean.getValue().equals(NO_JAVASCRIPT) == false){
					bean.setValue("<img width=\"500\" height=\"200\" src=\"" + bean.getValue() + "\">");
				}
				bean.setNameHoverText("Rendering of specific 3D forms following a fixed set of instructions."
						+ " The picture presents some slight noticeable variations depending on the device of the user.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "WebGLVendor", fingerprint.getWebGLVendor());
				bean.setName("WebGL Vendor");
				bean.setNameHoverText("Name of the WebGL Vendor. Some browsers give the full name of the underlying graphics card used by the device.");
				characteristics.add(bean);
			}
			{
				CharacteristicBean bean = getCharacteristicBean(conn, sampleCount, "WebGLRenderer", fingerprint.getWebGLRenderer());
				bean.setName("WebGL Renderer");
				bean.setNameHoverText("Name of the WebGL Renderer. Some browsers give the full name of the underlying graphics driver.");
				characteristics.add(bean);
			}

			return sampleID;

		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			// Close the connection
			// Finally triggers even if we return
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					// Ignore
				}
			}
		}
		return null;
	}

	/**
	 * Insert a sample into the Samples database.
	 * 
	 * @param conn
	 * @param fingerprint
	 * @return The sample ID of the inserted sample.
	 * @throws SQLException
	 */
	private static Integer insertSample(Connection conn, Fingerprint fingerprint) throws SQLException {
		PreparedStatement insertSample = conn.prepareStatement(insertSampleStr, Statement.RETURN_GENERATED_KEYS);
		int index = 1;
		insertSample.setString(index, fingerprint.getIpAddress());
		++index;
		insertSample.setString(index, fingerprint.getUser_agent());
		++index;
		insertSample.setString(index, fingerprint.getAccept_headers());
		++index;
		insertSample.setString(index, fingerprint.getPluginDetails());
		++index;
		insertSample.setString(index, fingerprint.getTimeZone());
		++index;
		insertSample.setString(index, fingerprint.getScreenDetails());
		++index;
		insertSample.setString(index, fingerprint.getFonts());
		++index;
		insertSample.setBoolean(index, fingerprint.isCookiesEnabled());
		++index;
		insertSample.setString(index, fingerprint.getSuperCookie());
		++index;
		insertSample.setString(index, fingerprint.getDoNotTrack());
		++index;
		if (fingerprint.getClockDifference() != null) {
			insertSample.setLong(index, fingerprint.getClockDifference());
		} else {
			insertSample.setNull(index, java.sql.Types.BIGINT);
		}
		++index;
		insertSample.setString(index, fingerprint.getDateTime());
		++index;
		insertSample.setString(index, fingerprint.getMathTan());
		++index;
		insertSample.setBoolean(index, fingerprint.isUsingTor());
		++index;
		if(fingerprint.getAdsBlocked() != null){
			insertSample.setBoolean(index, fingerprint.getAdsBlocked());
		}
		else{
			insertSample.setNull(index, java.sql.Types.BOOLEAN);
		}
		++index;
		insertSample.setString(index, fingerprint.getCanvas());
		++index;
		insertSample.setString(index, fingerprint.getWebGL());
		++index;
		insertSample.setString(index, fingerprint.getWebGLVendor());
		++index;
		insertSample.setString(index, fingerprint.getWebGLRenderer());

		insertSample.execute();

		ResultSet rs = insertSample.getGeneratedKeys();
		Integer sampleID = null;
		if (rs.next()) {
			sampleID = rs.getInt(1);
		}
		rs.close();
		insertSample.close();
		return sampleID;
	}
	
	/**
	 * 
	 * @param conn
	 * @param fingerprint
	 * @param sampleID
	 * @return
	 * @throws SQLException
	 */
	private static void insertSampleSet(Connection conn, Fingerprint fingerprint, Integer sampleID) throws SQLException {
		if(fingerprint.getSampleSetID() == null){
			/*
			 * Insert whole new SampleSetID.
			 */
			String query = "INSERT INTO `SampleSets`(`SampleID`) VALUES(?);";
			PreparedStatement insertSampleSet = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
			
			insertSampleSet.setInt(1, sampleID);
			insertSampleSet.execute();
			
			ResultSet rs = insertSampleSet.getGeneratedKeys();
			if (rs.next()) {
				fingerprint.setSampleSetID(rs.getInt(1));
			}
			rs.close();
			insertSampleSet.close();
		}
		else{
			/*
			 * Insert new SampleID for existing SampleSetID.
			 */
			String query = "INSERT INTO `SampleSets`(`SampleSetID`,`SampleID`) VALUES(?, ?);";
			PreparedStatement insertSampleSet = conn.prepareStatement(query);
			
			insertSampleSet.setInt(1, fingerprint.getSampleSetID());
			insertSampleSet.setInt(2, sampleID);
			insertSampleSet.execute();
			
			insertSampleSet.close();
		}
	}

	/**
	 * Returns the sampleID of the matching sample if we've seen this sample (with SampleSetID) before.
	 * Otherwise returns null.
	 * 
	 * @param conn
	 * @param fingerprint
	 * @return
	 * @throws SQLException
	 */
	private static Integer checkSampleChanged(Connection conn, Fingerprint fingerprint) throws SQLException {
		if(fingerprint.getSampleSetID() == null){
			/*
			 * We know we haven't seen this sample before because there's no SampleSetID.
			 */
			return null;
		}
		
		/*
		 * We have seen this user before. Check if their fingerprint has changed.
		 */
		String query = "SELECT `Samples`.`SampleID` FROM `SampleSets` INNER JOIN `Samples` ON `SampleSets`.`SampleID` = `Samples`.`SampleID` WHERE `SampleSetID` = ?"
				+ " AND `UserAgent`" + (fingerprint.getUser_agent() == null ? " IS NULL" : " = ?")
				+ " AND `AcceptHeaders`" + (fingerprint.getAccept_headers() == null ? " IS NULL" : " = ?")
				+ " AND `PluginDetails`" + (fingerprint.getPluginDetails() == null ? " IS NULL" : " = ?")
				+ " AND `TimeZone`" + (fingerprint.getTimeZone() == null ? " IS NULL" : " = ?")
				+ " AND `ScreenDetails`" + (fingerprint.getScreenDetails() == null ? " IS NULL" : " = ?")
				+ " AND `Fonts`" + (fingerprint.getFonts() == null ? " IS NULL" : " = ?")
				+ " AND `CookiesEnabled` = ?"
				+ " AND `SuperCookie`" + (fingerprint.getSuperCookie() == null ? " IS NULL" : " = ?")
				+ " AND `DoNotTrack`" + (fingerprint.getDoNotTrack() == null ? " IS NULL" : " = ?")
				+ " AND `ClockDifference`" + (fingerprint.getClockDifference() == null ? " IS NULL" : " = ?")
				+ " AND `DateTime`" + (fingerprint.getDateTime() == null ? " IS NULL" : " = ?")
				+ " AND `MathTan`" + (fingerprint.getMathTan() == null ? " IS NULL" : " = ?")
				+ " AND `UsingTor` = ?"
				+ " AND `AdsBlocked`" + (fingerprint.getAdsBlocked() == null ? " IS NULL" : " = ?")
				+ " AND `Canvas`" + (fingerprint.getCanvas() == null ? " IS NULL" : " = ?")
				+ " AND `WebGL`" + (fingerprint.getWebGL() == null ? " IS NULL" : " = ?")
				+ " AND `WebGLVendor`" + (fingerprint.getWebGLVendor() == null ? " IS NULL" : " = ?")
				+ " AND `WebGLRenderer`" + (fingerprint.getWebGLRenderer() == null ? " IS NULL" : " = ?")
				+ ";";
		PreparedStatement checkExists = conn.prepareStatement(query);
		
		int index = 1;
		checkExists.setInt(index, fingerprint.getSampleSetID());
		++index;

		if (fingerprint.getUser_agent() != null) {
			checkExists.setString(index, fingerprint.getUser_agent());
			++index;
		}
		if (fingerprint.getAccept_headers() != null) {
			checkExists.setString(index, fingerprint.getAccept_headers());
			++index;
		}
		if (fingerprint.getPluginDetails() != null) {
			checkExists.setString(index, fingerprint.getPluginDetails());
			++index;
		}
		if (fingerprint.getTimeZone() != null) {
			checkExists.setString(index, fingerprint.getTimeZone());
			++index;
		}
		if (fingerprint.getScreenDetails() != null) {
			checkExists.setString(index, fingerprint.getScreenDetails());
			++index;
		}
		if (fingerprint.getFonts() != null) {
			checkExists.setString(index, fingerprint.getFonts());
			++index;
		}
		checkExists.setBoolean(index, fingerprint.isCookiesEnabled());
		++index;
		if (fingerprint.getSuperCookie() != null) {
			checkExists.setString(index, fingerprint.getSuperCookie());
			++index;
		}
		if (fingerprint.getDoNotTrack() != null) {
			checkExists.setString(index, fingerprint.getDoNotTrack());
			++index;
		}
		if (fingerprint.getClockDifference() != null) {
			checkExists.setLong(index, fingerprint.getClockDifference());
			++index;
		}
		if (fingerprint.getDateTime() != null) {
			checkExists.setString(index, fingerprint.getDateTime());
			++index;
		}
		if (fingerprint.getMathTan() != null) {
			checkExists.setString(index, fingerprint.getMathTan());
			++index;
		}
		checkExists.setBoolean(index, fingerprint.isUsingTor());
		++index;
		if (fingerprint.getAdsBlocked() != null) {
			checkExists.setBoolean(index, fingerprint.getAdsBlocked());
			++index;
		}
		if (fingerprint.getCanvas() != null) {
			checkExists.setString(index, fingerprint.getCanvas());
			++index;
		}
		if (fingerprint.getWebGL() != null) {
			checkExists.setString(index, fingerprint.getWebGL());
			++index;
		}
		if (fingerprint.getWebGLVendor() != null) {
			checkExists.setString(index, fingerprint.getWebGLVendor());
			++index;
		}
		if (fingerprint.getWebGLRenderer() != null) {
			checkExists.setString(index, fingerprint.getWebGLRenderer());
			++index;
		}

		ResultSet rs = checkExists.executeQuery();

		Integer sampleID = null;
		if (rs.next()) {
			/*
			 * We've seen this sample before and the fingerprint hasn't changed,
			 * don't log it.
			 */
			sampleID = rs.getInt(1);
		}
		rs.close();
		checkExists.close();
		return sampleID;
	}

	private static int getSampleCount(Connection conn) throws SQLException {
		PreparedStatement getSampleCount = conn.prepareStatement(getSampleCountStr);
		ResultSet rs = getSampleCount.executeQuery();
		rs.next();
		int sampleCount = rs.getInt(1);
		rs.close();
		return sampleCount;
	}

	/**
	 * Check whether a fingerprint with all the given details, including matching SampleID,
	 * is already inside the database.
	 * 
	 * @param conn
	 * @param fingerprint
	 * @return
	 * @throws SQLException
	 */
	private static int getSampleOccurrences(Connection conn, Fingerprint fingerprint) throws SQLException {
		/*
		 * We have seen this user before. Check if their fingerprint has changed.
		 */
		String query = "SELECT COUNT(*) FROM `Samples` WHERE"
				+ " `UserAgent`" + (fingerprint.getUser_agent() == null ? " IS NULL" : " = ?")
				+ " AND `AcceptHeaders`" + (fingerprint.getAccept_headers() == null ? " IS NULL" : " = ?")
				+ " AND `PluginDetails`" + (fingerprint.getPluginDetails() == null ? " IS NULL" : " = ?")
				+ " AND `TimeZone`" + (fingerprint.getTimeZone() == null ? " IS NULL" : " = ?")
				+ " AND `ScreenDetails`" + (fingerprint.getScreenDetails() == null ? " IS NULL" : " = ?")
				+ " AND `Fonts`" + (fingerprint.getFonts() == null ? " IS NULL" : " = ?")
				+ " AND `CookiesEnabled` = ?"
				+ " AND `SuperCookie`" + (fingerprint.getSuperCookie() == null ? " IS NULL" : " = ?")
				+ " AND `DoNotTrack`" + (fingerprint.getDoNotTrack() == null ? " IS NULL" : " = ?")
				+ " AND `ClockDifference`" + (fingerprint.getClockDifference() == null ? " IS NULL" : " = ?")
				+ " AND `DateTime`" + (fingerprint.getDateTime() == null ? " IS NULL" : " = ?")
				+ " AND `MathTan`" + (fingerprint.getMathTan() == null ? " IS NULL" : " = ?")
				+ " AND `UsingTor` = ?"
				+ " AND `AdsBlocked`" + (fingerprint.getAdsBlocked() == null ? " IS NULL" : " = ?")
				+ " AND `Canvas`" + (fingerprint.getCanvas() == null ? " IS NULL" : " = ?")
				+ " AND `WebGL`" + (fingerprint.getWebGL() == null ? " IS NULL" : " = ?")
				+ " AND `WebGLVendor`" + (fingerprint.getWebGLVendor() == null ? " IS NULL" : " = ?")
				+ " AND `WebGLRenderer`" + (fingerprint.getWebGLRenderer() == null ? " IS NULL" : " = ?")
				+ ";";
		PreparedStatement checkExists = conn.prepareStatement(query);

		int index = 1;
		if (fingerprint.getUser_agent() != null) {
			checkExists.setString(index, fingerprint.getUser_agent());
			++index;
		}
		if (fingerprint.getAccept_headers() != null) {
			checkExists.setString(index, fingerprint.getAccept_headers());
			++index;
		}
		if (fingerprint.getPluginDetails() != null) {
			checkExists.setString(index, fingerprint.getPluginDetails());
			++index;
		}
		if (fingerprint.getTimeZone() != null) {
			checkExists.setString(index, fingerprint.getTimeZone());
			++index;
		}
		if (fingerprint.getScreenDetails() != null) {
			checkExists.setString(index, fingerprint.getScreenDetails());
			++index;
		}
		if (fingerprint.getFonts() != null) {
			checkExists.setString(index, fingerprint.getFonts());
			++index;
		}
		checkExists.setBoolean(index, fingerprint.isCookiesEnabled());
		++index;
		if (fingerprint.getSuperCookie() != null) {
			checkExists.setString(index, fingerprint.getSuperCookie());
			++index;
		}
		if (fingerprint.getDoNotTrack() != null) {
			checkExists.setString(index, fingerprint.getDoNotTrack());
			++index;
		}
		if (fingerprint.getClockDifference() != null) {
			checkExists.setLong(index, fingerprint.getClockDifference());
			++index;
		}
		if (fingerprint.getDateTime() != null) {
			checkExists.setString(index, fingerprint.getDateTime());
			++index;
		}
		if (fingerprint.getMathTan() != null) {
			checkExists.setString(index, fingerprint.getMathTan());
			++index;
		}
		checkExists.setBoolean(index, fingerprint.isUsingTor());
		++index;
		if (fingerprint.getAdsBlocked() != null) {
			checkExists.setBoolean(index, fingerprint.getAdsBlocked());
			++index;
		}
		if (fingerprint.getCanvas() != null) {
			checkExists.setString(index, fingerprint.getCanvas());
			++index;
		}
		if (fingerprint.getWebGL() != null) {
			checkExists.setString(index, fingerprint.getWebGL());
			++index;
		}
		if (fingerprint.getWebGLVendor() != null) {
			checkExists.setString(index, fingerprint.getWebGLVendor());
			++index;
		}
		if (fingerprint.getWebGLRenderer() != null) {
			checkExists.setString(index, fingerprint.getWebGLRenderer());
			++index;
		}

		ResultSet rs = checkExists.executeQuery();

		rs.next();
		int count = rs.getInt(1);
		rs.close();
		checkExists.close();
		return count;
	}

	/**
	 * Create the cookies enabled CharacteristicBean.
	 * 
	 * @param conn
	 *            A connection to the database.
	 * @param num_samples
	 *            The number of samples in the database.
	 * @param value
	 *            The value of this sample.
	 * @return
	 * @throws SQLException
	 */
	private static CharacteristicBean getCharacteristicBean(Connection conn, int num_samples, String dbname, Boolean value) throws SQLException {
		CharacteristicBean chrbean = new CharacteristicBean();

		PreparedStatement getCount;
		String querystr = "SELECT COUNT(*) FROM `Samples` WHERE `" + dbname + "`";
		if (value != null) {
			if (value){
				chrbean.setValue("Yes");
			}
			else {
				chrbean.setValue("No");
			}
			querystr += " = ?;";

			getCount = conn.prepareStatement(querystr);
			getCount.setBoolean(1, value);
		}
		else {
			chrbean.setValue(NO_JAVASCRIPT);

			querystr += " IS NULL;";
			getCount = conn.prepareStatement(querystr);
		}

		ResultSet rs = getCount.executeQuery();
		rs.next();
		int count = rs.getInt(1);
		rs.close();

		chrbean.setInX(((double) num_samples) / ((double) count));
		chrbean.setBits(Math.abs(Math.log(chrbean.getInX()) / Math.log(2)));

		return chrbean;
	}

	/**
	 * Create a CharacteristicBean.
	 * 
	 * @param conn
	 *            A connection to the database.
	 * @param num_samples
	 *            The number of samples in the database.
	 * @param value
	 *            The value of this sample.
	 * @return
	 * @throws SQLException
	 */
	private static CharacteristicBean getCharacteristicBean(Connection conn, int num_samples, String dbname, String value) throws SQLException {
		CharacteristicBean chrbean = new CharacteristicBean();

		PreparedStatement getCount;
		String querystr = "SELECT COUNT(*) FROM `Samples` WHERE `" + dbname + "`";
		if (value != null) {
			chrbean.setValue(StringEscapeUtils.escapeHtml4(value));
			querystr += " = ?;";

			getCount = conn.prepareStatement(querystr);
			getCount.setString(1, value);
		}
		else {
			chrbean.setValue(NO_JAVASCRIPT);

			querystr += " IS NULL;";
			getCount = conn.prepareStatement(querystr);
		}

		ResultSet rs = getCount.executeQuery();
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		chrbean.setInX(((double) num_samples) / ((double) count));
		chrbean.setBits(Math.abs(Math.log(chrbean.getInX()) / Math.log(2)));

		return chrbean;
	}

	/**
	 * Create a CharacteristicBean.
	 * 
	 * @param conn
	 *            A connection to the database.
	 * @param num_samples
	 *            The number of samples in the database.
	 * @param value
	 *            The value of this sample.
	 * @return
	 * @throws SQLException
	 */
	private static CharacteristicBean getCharacteristicBean(Connection conn, int num_samples, String dbname, Long value) throws SQLException {
		CharacteristicBean chrbean = new CharacteristicBean();

		PreparedStatement getCount;
		String querystr = "SELECT COUNT(*) FROM `Samples` WHERE `" + dbname + "`";
		if (value != null) {
			chrbean.setValue(value.toString());
			querystr += " = ?;";

			getCount = conn.prepareStatement(querystr);
			getCount.setLong(1, value);
		}
		else {
			chrbean.setValue(NO_JAVASCRIPT);

			querystr += " IS NULL;";
			getCount = conn.prepareStatement(querystr);
		}

		ResultSet rs = getCount.executeQuery();
		rs.next();
		int count = rs.getInt(1);
		rs.close();
		chrbean.setInX(((double) num_samples) / ((double) count));
		chrbean.setBits(Math.abs(Math.log(chrbean.getInX()) / Math.log(2)));

		return chrbean;
	}
}