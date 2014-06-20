package org.envirocar.wps.util;

import java.text.ParseException;
import java.util.Date;

import org.envirocar.wps.util.EnviroCarWpsConstants.FeatureProperties;
import org.opengis.feature.simple.SimpleFeature;

import com.fasterxml.jackson.databind.util.ISO8601DateFormat;

/**
 * contains basic methods for calculating statistics for tracks/features
 * 
 * @author staschc
 * 
 */
public class BasicStats {

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
	public static double calculateAcceleration(SimpleFeature feat1,
			SimpleFeature feat2) throws ParseException {
		double acceleration = 0;
		
		//compute delta t in seconds
		ISO8601DateFormat df = new ISO8601DateFormat();
		Date timeStart = df.parse((String)feat1.getAttribute(FeatureProperties.TIME));
		Date timeEnd = df.parse((String)feat2.getAttribute(FeatureProperties.TIME));
		long delta_t = (timeEnd.getTime()-timeStart.getTime()) * 1000;
		
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
