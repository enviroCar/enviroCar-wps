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

import org.envirocar.wps.util.BasicStats;
import org.envirocar.wps.util.EnviroCarFeatureParser;
import org.envirocar.wps.util.EnviroCarWpsConstants;
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
				while (featIter.hasNext()){
					SimpleFeature feat1 = featIter.next();
					if (featIter.hasNext()){
						SimpleFeature feat2 = featIter.next();
						sum += BasicStats.calculateAcceleration(feat1, feat2);
						counter++;
					}
					
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
	    
	    

}
