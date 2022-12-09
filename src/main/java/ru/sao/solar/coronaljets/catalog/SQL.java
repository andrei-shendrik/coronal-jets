package ru.sao.solar.coronaljets.catalog;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.invoke.MethodHandles;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class SQL {
	
	final static Logger logger = LogManager.getLogger(MethodHandles.lookup().lookupClass());
	
	public static void getEventID(Connection conn, String query) {
		
		try {
			PreparedStatement ps = conn.prepareStatement(query);
			
			// process the results
		    ResultSet rs = ps.executeQuery();
		    while ( rs.next() )
		    {

		    }
		    rs.close();
		    ps.close();
		} catch (SQLException e) {
			logger.error(e.getMessage(),e);
		}
		
	    
		
	}

}
