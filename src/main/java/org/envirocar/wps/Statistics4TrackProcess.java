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
import java.text.ParseException;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import net.opengis.wps.x100.OutputDefinitionType;

import org.envirocar.wps.util.EnviroCarFeatureParser;
import org.envirocar.wps.util.EnviroCarWpsConstants;
import org.envirocar.wps.util.EnviroCarWpsConstants.FeatureProperties;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.commons.context.ExecutionContextFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;
import com.vividsolutions.jts.geom.Point;

/**
 * Computes the stops of envirocar tracks at points of interest
 * 
 * @author Christoph Stasch, Benjamin Pross
 *
 */
@Algorithm(version = "1.0.0")
public class Statistics4TrackProcess {
		
	   private static Logger LOGGER = LoggerFactory.getLogger(Statistics4TrackProcess.class);
	   
	   /**
	    * constants for output identifiers
	    * 
	    * @author staschc
	    *
	    */
	   private abstract class OutputIDs{
		   public final static String TRAVEL_TIME="travelTime";
		   public final static String MEAN_SPEED="meanSpeed";
		   public static final String TRAVEL_DISTANCE = "travelDistance";
		   public static final String MEAN_ACCELERATION = "meanAcceleration";
	   }
	 	
	    /** track ID for which statistics should be computed */
	    private String trackID;
	    
	    /** outputs*/
	    private double travelTime, speed, distance, acceleration;
	    
	    /**stores the ids of the requested outputs*/
	    private Collection<String> requestedOutputIDs;

	    
	    /**
		 * constructor
		 */
	    public Statistics4TrackProcess() {
	        super();
	    }
	    
	    //TODO is comma-seperated list at the moment; check with Benjamin how to support multiple outputs using annotations
	    @LiteralDataOutput(identifier = OutputIDs.TRAVEL_TIME)
	    public double getTravelTime() {
	        return travelTime;
	    }
	    
	    @LiteralDataOutput(identifier = OutputIDs.MEAN_SPEED)
	    public double getMeanSpeed() {
	        return speed;
	    }
	    
	    @LiteralDataOutput(identifier = OutputIDs.TRAVEL_DISTANCE)
	    public double getTravelDistance() {
	        return distance;
	    }
	    
	    @LiteralDataOutput(identifier = OutputIDs.MEAN_ACCELERATION)
	    public double getMeanAcceleration() {
	        return acceleration;
	    }
	    

	    @LiteralDataInput(identifier = "trackID", abstrakt="Specify the size of the buffer that is used to identify stops around a traffic light.")
	    public void setBufferSize(String trackID) {
	        this.trackID = trackID;
	    }
	    
	    @Execute
		public void computeStatistics() throws Exception {
	    	extractRequestedOutputs();
	    	getStatistics4Track(this.trackID);
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
		public void getStatistics4Track(String trackID2) throws Exception {
			
			//fetch track from enviroCar server
			URL url = new URL(EnviroCarWpsConstants.ENV_SERVER_URL+"/tracks/"+trackID2);
			EnviroCarFeatureParser parser = new EnviroCarFeatureParser();
			SimpleFeatureCollection features = parser.createFeaturesFromJSON(url);
			
			//initialize iterator and local variables
			SimpleFeatureIterator featIter = features.features();
			Date begin = null;
			Date end = null;
			ISO8601DateFormat df = new ISO8601DateFormat();
			int count = 0;
			double speedSum = 0, accelerationSum = 0;
			this.distance = 0;
			try {
				SimpleFeature feat ,prevFeat = null;
				while (featIter.hasNext()){
					feat = featIter.next();
					
					//fetch times for computing travel time
					if (this.requestedOutputIDs.contains(OutputIDs.TRAVEL_TIME)){
						if (begin==null){
							begin = df.parse((String)feat.getAttribute(FeatureProperties.TIME));
						}
						end = df.parse((String)feat.getAttribute(FeatureProperties.TIME));
					}
					
					//calculate speed
					String speedString = (String)feat.getAttribute(FeatureProperties.SPEED);
					if (this.requestedOutputIDs.contains(OutputIDs.MEAN_SPEED) && speedString!=null){
						speedSum += Double.parseDouble(speedString);
					}
					
					//calculate travel distance
					if (this.requestedOutputIDs.contains(OutputIDs.TRAVEL_DISTANCE) && prevFeat!=null){
						this.distance += calculateDistance(feat,prevFeat);
					}
					
					//calculate acceleration
					if (this.requestedOutputIDs.contains(OutputIDs.MEAN_ACCELERATION) && prevFeat!=null){
						accelerationSum += calculateAcceleration(prevFeat, feat);
					}
					
					prevFeat = feat;
					count++;
				}
			} catch (Exception e){
				LOGGER.debug("Error while extracting speed attribute from features: "+e.getLocalizedMessage());
				throw(e);
			}
			finally{
				featIter.close();
			}
			
			if (this.requestedOutputIDs.contains(OutputIDs.TRAVEL_TIME)){
				this.travelTime = (end.getTime()-begin.getTime()) / 60000;
			}
			
			this.speed = speedSum/count;
			this.acceleration = accelerationSum/count;
		}

		/**
		 * 
		 * calculates the distance in meters between two point features
		 * 
		 * @param feat
		 * 			destination feature
		 * @param prevFeat
		 * 			origin feature
		 * @return
		 * @throws TransformException
		 * 				if transformation from point feature geometries to direct positions fails
		 * @throws NoSuchAuthorityCodeException
		 * 				if fetching of CRS fails
		 * @throws FactoryException
		 * 				if creating the GeodeticCalculator fails
		 */
		private double calculateDistance(SimpleFeature feat,
				SimpleFeature prevFeat) throws TransformException, NoSuchAuthorityCodeException, FactoryException {
			//TODO works currently only for WGS 84
			CoordinateReferenceSystem wgsCRS = CRS.decode("EPSG:4326");
			GeodeticCalculator gc = new GeodeticCalculator(wgsCRS);
			Point start = (Point)prevFeat.getDefaultGeometry();
			Point end = (Point)feat.getDefaultGeometry();
			gc.setStartingPosition( JTS.toDirectPosition( start.getCoordinate(), wgsCRS ) );
			gc.setDestinationPosition( JTS.toDirectPosition( end.getCoordinate(), wgsCRS ) );
			return gc.getOrthodromicDistance();
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
		private double calculateAcceleration(SimpleFeature feat1,
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
	    
		
		/**
		 * extracts the identifiers of the requested outputs; identifiers are fetched to avoid unneeded
		 * computations
		 */
		private void extractRequestedOutputs(){
			List<OutputDefinitionType> reqOutputsList = ExecutionContextFactory.getContext().getOutputs();
			this.requestedOutputIDs=new HashSet<String>(reqOutputsList.size());
			for (OutputDefinitionType odt:reqOutputsList){
				this.requestedOutputIDs.add(odt.getIdentifier().getStringValue());
			}
		}
}
