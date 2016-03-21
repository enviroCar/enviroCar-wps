/**
 * Copyright (C) ${year}
 * by 52 North Initiative for Geospatial Open Source Software GmbH
 *
 * Contact: Andreas Wytzisk
 * 52 North Initiative for Geospatial Open Source Software GmbH
 * Martin-Luther-King-Weg 24
 * 48155 Muenster, Germany
 * info@52north.org
 *
 * This program is free software; you can redistribute and/or modify it under
 * the terms of the GNU General Public License version 2 as published by the
 * Free Software Foundation.
 *
 * This program is distributed WITHOUT ANY WARRANTY; even without the implied
 * WARRANTY OF MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * this program (see gnu-gpl v2.txt). If not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA or
 * visit the Free Software Foundation web page, http://www.fsf.org.
 */

/**
 * class represents statistics of eC measurements for a specific time window
 * 
 * @author Julius Wittkopp
 *
 */
package org.envirocar.wps.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import org.envirocar.wps.StatsForPOI;

import java.util.Collection;

import com.google.common.collect.SetMultimap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;



public class CsvFileWriter {
	
	//Delimiter used in CSV file
	private static final String SEMICOLON_DELIMITER = ";";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String FILE_HEADER = "Tag;Tagesmittel;Fahrten;Gesamtmittel";
	private static Collection<Object> mean;
	private static Logger LOGGER = LoggerFactory.getLogger(CsvFileWriter.class);
	
	public static File writeCsvFile(String fileName, SetMultimap<String, Object> finalStatistics) {
		
		 int dayMean = 0;
		 mean = finalStatistics.values();
		 int meanTotal = 0;
		 int meanColSize = mean.size();
		 if(meanColSize == 0){
			 meanColSize = 1;
		 }
		 
		 for (Object elem : mean) {
			 String el = String.valueOf(elem);
			 meanTotal = meanTotal + Integer.parseInt(el.trim());
		    }
		
		 meanTotal= meanTotal/meanColSize;
		
		 FileWriter fileWriter = null;
		 
		 try{
			 fileWriter = new FileWriter(fileName);
			 
			 fileWriter.append(FILE_HEADER.toString()); 
			 fileWriter.append(NEW_LINE_SEPARATOR);			 				 
			 for(Object key : finalStatistics.keySet()){
				 fileWriter.append(String.valueOf(key));
				 fileWriter.append(SEMICOLON_DELIMITER);				 
				 	for(Object val : finalStatistics.get((String) key)){ 
				 		 String el = String.valueOf(val);
				 	  	 dayMean = (dayMean + Integer.parseInt(el.trim()));
				 	}
				 String amountDayTracks = String.valueOf(finalStatistics.get((String) key).size());
				 dayMean = dayMean/Integer.parseInt(amountDayTracks.trim());
				 fileWriter.append(String.valueOf(dayMean));				 
				 dayMean = 0;
				 // fileWriter.append(String.valueOf(finalStatistics.get((String) key)));
				 fileWriter.append(SEMICOLON_DELIMITER);
				 fileWriter.append(String.valueOf(finalStatistics.get((String) key).size()));
				 fileWriter.append(SEMICOLON_DELIMITER);
				 fileWriter.append(NEW_LINE_SEPARATOR);					    
				}
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(String.valueOf(finalStatistics.values().size()));
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(String.valueOf(meanTotal));
			 
			 
			 LOGGER.info("CSV file was created successfully !!!");
		 }catch (Exception e) {
			 LOGGER.error("Error in CsvFileWriter !!!");
			 e.printStackTrace();
		 } finally {
			 try {
				 fileWriter.flush();
				 fileWriter.close();
			 } catch (IOException e) {
				 LOGGER.error("Error while flushing/closing fileWriter !!!");
				 e.printStackTrace();
			 }
		 }
		
		return new File(fileName);
	}
}
