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
package org.envirocar.wps;

import java.net.URL;
import java.text.ParseException;
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
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
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
	public class Acceleration4TrackProcess extends AbstractAnnotatedAlgorithm {

	    private static Logger LOGGER = LoggerFactory.getLogger(Acceleration4TrackProcess.class);
	    

		/**
		 * constructor
		 */
	    public Acceleration4TrackProcess() {
	        super();
	    }
	    
	    private String trackID;
	    private double result;

	    @LiteralDataOutput(identifier = "meanAcceleration")
	    public double getResult() {
	        return result;
	    }

	    @LiteralDataInput(identifier = "trackID", abstrakt="Specify the size of the buffer that is used to identify stops around a traffic light.")
	    public void setBufferSize(String trackID) {
	        this.trackID = trackID;
	    }
	    
	    @Execute
		public void computeAcceleration() throws Exception {
	    	this. result = getAcceleration4Track(this.trackID);
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
		public double getAcceleration4Track(String trackID2) throws Exception {
			double sum = 0;
			int counter = 0;
			URL url = new URL(EnviroCarWpsConstants.ENV_SERVER_URL+"/tracks/"+trackID2);
			EnviroCarFeatureParser parser = new EnviroCarFeatureParser();
			SimpleFeatureCollection features = parser.createFeaturesFromJSON(url);
			SimpleFeatureIterator featIter = features.features();
			try {
				SimpleFeature featFirst = featIter.next();
				while (featIter.hasNext()){
					SimpleFeature featSecond = featIter.next();
					sum += calculateAcceleration(featFirst, featSecond);
					featFirst=featSecond;
					counter++;
				}
			} catch (Exception e){
				LOGGER.debug("Error while extracting speed attribute from features: "+e.getLocalizedMessage());
				throw(e);
			}
			finally{
				featIter.close();
			}
			return sum/counter;
		}	
		
		/**
		 * calculates acceleration (delta_speed/delta_time in m/s^2) between two single
		 * track measurements, represented as Geotools simple features
		 * 
		 * 
		 * @param feat1
		 *            first track measurement (must be temporally before second)
		 * @param feat2
		 *            second track measurement
		 * @return acceleration between first and second measurement
		 * @throws ParseException 
		 */
		public double calculateAcceleration(SimpleFeature feat1,
				SimpleFeature feat2) throws ParseException {
			double acceleration = 0;
			
			//compute delta t in seconds
			ISO8601DateFormat df = new ISO8601DateFormat();
			Date timeStart = df.parse((String)feat1.getAttribute(FeatureProperties.TIME));
			Date timeEnd = df.parse((String)feat2.getAttribute(FeatureProperties.TIME));
			long delta_t = (timeEnd.getTime()-timeStart.getTime()) / 1000;
			
			//compute delta speed in m/s
			double delta_speed = 0;
			Object spo1 = feat1.getAttribute(FeatureProperties.SPEED);
			Object spo2 = feat2.getAttribute(FeatureProperties.SPEED);
			
			//compute delta speed in km/h
			if (spo1!=null && spo2!=null){
				 double speed1 = Double.parseDouble((String)spo1);
				 double speed2 = Double.parseDouble((String)spo2);
				if (speed2>speed1){
					delta_speed= speed2-speed1;
				}
			}
			
			//convert to m/s
			delta_speed = delta_speed * 0.278;
			
			//compute acceleration in m/s^2
			acceleration = delta_speed/delta_t;
			
			return acceleration;
		}
	    
	    

}
