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
package org.envirocar.wps.test;


import org.envirocar.wps.StatsForPOI;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;

public class StatsForPOITest {
	
	private Geometry p1;
	private double bufferSize;
	private String day = "Mittwoch,Donnerstag,Freitag";
	private int timeWindowEnd = 0;
	private int timeWindowStart = 0;
	

	@Test
	public void test() throws ParseException {			    
		this.bufferSize = 3000.0;
		this.p1 =  new GeometryFactory().createPoint(new Coordinate(51.95581065461393,7.626421451568604));
		try {
			new StatsForPOI().simpleAlgorithm(p1, bufferSize,day,timeWindowStart,timeWindowEnd);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
