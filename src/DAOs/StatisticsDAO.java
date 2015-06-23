package DAOs;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.json.JSONObject;

import datastructures.Fingerprint;
import eu.bitwalker.useragentutils.UserAgent;

public class StatisticsDAO {

	public static final void saveStatistics(int sampleID, Fingerprint fingerprint){
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(false);

			String query = "INSERT INTO `SampleStatistics` (`SampleID`, `BrowserGroup`, `BrowserVersion`, `OSGroup`, `OSName`) VALUES(?, ?, ?, ?, ?);";
			PreparedStatement insertStatistics = conn.prepareStatement(query);
			
			insertStatistics.setInt(1, sampleID);
			
			UserAgent ua = new UserAgent(fingerprint.getUser_agent());
			insertStatistics.setString(2, ua.getBrowser().getGroup().toString());
			insertStatistics.setString(3, ua.getBrowserVersion().getMajorVersion());
			insertStatistics.setString(4, ua.getOperatingSystem().getGroup().toString());
			insertStatistics.setString(5, ua.getOperatingSystem().getName());
			
			insertStatistics.execute();
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
		return;
	}
	
	public static final String getPercentageTorUsers() {
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(true);

			String query = "SELECT `UsingTor`, COUNT(*) FROM `Samples` GROUP BY `UsingTor`;";
			PreparedStatement select = conn.prepareStatement(query);

			ResultSet rs = select.executeQuery();

			JSONObject results = new JSONObject();
			while(rs.next()){
				boolean usingTor = rs.getBoolean(1);
				String usingTorStr;
				if(usingTor){
					usingTorStr = "1";
				}
				else{
					usingTorStr = "0";
				}
				
				int count = rs.getInt(2);
				results.put(usingTorStr, count);
			}
			rs.close();
			select.close();
			
			return results.toString();
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
	
	public static final String getOSBreakdown() {
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(true);

			String query = "SELECT `OSGroup`, `OSName`, COUNT(*) FROM `SampleStatistics` GROUP BY `OSGroup`, `OSName`;";
			PreparedStatement select = conn.prepareStatement(query);

			ResultSet rs = select.executeQuery();

			HashMap<String, JSONObject> groups = new HashMap<String, JSONObject>();
			while(rs.next()){
				String groupName = rs.getString(1);
				String name = rs.getString(2);
				int count = rs.getInt(3);
				
				JSONObject group = groups.get(groupName);
				if(group == null){
					group = new JSONObject();
					groups.put(groupName, group);
				}
				group.put(name, count);
			}
			rs.close();
			select.close();
			
			JSONObject results = new JSONObject();
			for(String groupName: groups.keySet()){
				results.put(groupName, groups.get(groupName));
			}
			
			return results.toString();
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
	
	public static final String getBrowserBreakdown() {
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(true);

			String query = "SELECT `BrowserGroup`, `BrowserVersion`, COUNT(*) FROM `SampleStatistics` GROUP BY `BrowserGroup`, `BrowserVersion`;";
			PreparedStatement select = conn.prepareStatement(query);

			ResultSet rs = select.executeQuery();

			HashMap<String, JSONObject> groups = new HashMap<String, JSONObject>();
			while(rs.next()){
				String groupName = rs.getString(1);
				String name = rs.getString(2);
				int count = rs.getInt(3);
				
				JSONObject group = groups.get(groupName);
				if(group == null){
					group = new JSONObject();
					groups.put(groupName, group);
				}
				group.put(name, count);
			}
			rs.close();
			select.close();
			
			JSONObject results = new JSONObject();
			for(String groupName: groups.keySet()){
				results.put(groupName, groups.get(groupName));
			}
			
			return results.toString();
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
	
	public static final String getTimezones() {
		Connection conn = null;
		try {
			conn = Database.getConnection();
			conn.setReadOnly(true);

			String query = "SELECT `TimeZone`, COUNT(*) FROM `Samples` GROUP BY `TimeZone`;";
			PreparedStatement select = conn.prepareStatement(query);

			ResultSet rs = select.executeQuery();

			JSONObject results = new JSONObject();
			while(rs.next()){
				int timezone = rs.getInt(1);
				String timezoneStr;
				if(rs.wasNull()){
					timezoneStr = "No JS";
				}
				else{
					timezoneStr = Integer.toString(timezone);
				}
				
				int count = rs.getInt(2);
				results.put(timezoneStr, count);
			}
			rs.close();
			select.close();
			
			return results.toString();
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
}