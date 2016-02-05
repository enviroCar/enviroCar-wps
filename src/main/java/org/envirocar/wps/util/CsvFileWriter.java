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

package org.envirocar.wps.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.google.common.collect.SetMultimap;

import org.envirocar.wps.StatsForPOI;

public class CsvFileWriter {
	
	//Delimiter used in CSV file
	private static final String SEMICOLON_DELIMITER = ";";
	private static final String NEW_LINE_SEPARATOR = "\n";
	private static final String FILE_HEADER = "Day;Measurement;AmountOfTracks;AmountOfMeasurements";

	
	public static File writeCsvFile(String fileName, SetMultimap<String, Object> finalStatistics) {
		
		 FileWriter fileWriter = null;
		 
		 
		 try{
			 fileWriter = new FileWriter(fileName);
			 
			 fileWriter.append(FILE_HEADER.toString()); 
			 fileWriter.append(NEW_LINE_SEPARATOR);			 				 
			 for(Object key : finalStatistics.keySet()){
				 fileWriter.append(String.valueOf(key));
				 fileWriter.append(SEMICOLON_DELIMITER);
				 fileWriter.append(String.valueOf(finalStatistics.get((String) key)));
				 fileWriter.append(SEMICOLON_DELIMITER);
				 fileWriter.append(SEMICOLON_DELIMITER);
				 fileWriter.append(NEW_LINE_SEPARATOR);					    
				}
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(String.valueOf(StatsForPOI.numberOfTracks));
			 fileWriter.append(SEMICOLON_DELIMITER);
			 fileWriter.append(String.valueOf(StatsForPOI.TotalAmountOfPoints));
			 fileWriter.append(NEW_LINE_SEPARATOR);	
			 
			 
			 System.out.println("CSV file was created successfully !!!");
		 }catch (Exception e) {
			 System.out.println("Error in CsvFileWriter !!!");
			 e.printStackTrace();
		 } finally {
			 try {
				 fileWriter.flush();
				 fileWriter.close();
			 } catch (IOException e) {
				 System.out.println("Error while flushing/closing fileWriter !!!");
				 e.printStackTrace();
			 }
		 }
		
		return new File(fileName);
	}
}
