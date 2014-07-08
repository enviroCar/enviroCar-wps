package org.envirocar.wps;
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
import java.net.URL;
import java.util.Date;

import org.envirocar.wps.util.EnviroCarFeatureParser;
import org.envirocar.wps.util.EnviroCarWpsConstants;
import org.envirocar.wps.util.EnviroCarWpsConstants.FeatureProperties;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.opengis.feature.simple.SimpleFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

/**
 * Computes the stops of envirocar tracks at points of interest
 * 
 * @author Christoph Stasch, Benjamin Pross
 *
 */
@Algorithm(version = "1.0.0")
public class Statistics4TrackProcess {
	
	 private static Logger LOGGER = LoggerFactory.getLogger(Acceleration4TrackProcess.class);
	    

		/**
		 * constructor
		 */
	    public Statistics4TrackProcess() {
	        super();
	    }
	    
	    private String trackID;
	    private String result;

	    //TODO is comma-seperated list at the moment; check with Benjamin how to support multiple outputs using annotations
	    @LiteralDataOutput(identifier = "statistics")
	    public String getResult() {
	        return result;
	    }
	    
	    

	    @LiteralDataInput(identifier = "trackID", abstrakt="Specify the size of the buffer that is used to identify stops around a traffic light.")
	    public void setBufferSize(String trackID) {
	        this.trackID = trackID;
	    }
	    
	    @Execute
		public void computeStatistics() throws Exception {
	    	this.result = getStatistics4Track(this.trackID);
		}

	    
	    /**
	     * computes mean acceleration for a single track
	     * 
	     * @param trackID2
	     * 			ID of track for which mean acceleration should be computed 
	     * @return mean acceleration of track
	     * @throws Exception
	     * 			if parsing of track fails
	     */
		public String getStatistics4Track(String trackID2) throws Exception {
			String result = "";
			URL url = new URL(EnviroCarWpsConstants.ENV_SERVER_URL+"/tracks/"+trackID2);
			EnviroCarFeatureParser parser = new EnviroCarFeatureParser();
			SimpleFeatureCollection features = parser.createFeaturesFromJSON(url);
			SimpleFeatureIterator featIter = features.features();
			Date begin = null;
			Date end = null;
			ISO8601DateFormat df = new ISO8601DateFormat();
			int count = 0;
			double speedSum = 0;
			try {
				while (featIter.hasNext()){
					SimpleFeature feat = featIter.next();
					//fetch times for computing travel time
					if (begin==null){
						begin = df.parse((String)feat.getAttribute(FeatureProperties.TIME));
					}
					end = df.parse((String)feat.getAttribute(FeatureProperties.TIME));
					String speedString = (String)feat.getAttribute(FeatureProperties.SPEED);
					if (speedString!=null){
						speedSum += Double.parseDouble(speedString);
					}
					count++;
				}
			} catch (Exception e){
				LOGGER.debug("Error while extracting speed attribute from features: "+e.getLocalizedMessage());
				throw(e);
			}
			finally{
				featIter.close();
			}
			
			long travelTime = (end.getTime()-begin.getTime()) / 60000;
			double meanSpeed = speedSum/count;
			result += "Travel Time: " + travelTime + ", Average Speed: " + meanSpeed;
			return result;
		}	
}
