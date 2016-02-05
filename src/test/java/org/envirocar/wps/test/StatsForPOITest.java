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

import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;

import org.envirocar.wps.StatsForPOI;
import org.junit.Test;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class StatsForPOITest {
	
	private Geometry p1;
	private double bufferSize;
	

	@Test
	public void test() throws ParseException {
		
		Geometry g = new WKTReader().read(new InputStreamReader(new ByteArrayInputStream("POINT(51.752703 7.317661)".getBytes())));
			    
		this.bufferSize = 8000.0;
		this.p1 =  new GeometryFactory().createPoint(new Coordinate(51.752703,7.317661));
		//51.752703,7.317661 //Haltern
		//51.212693,6.357593 //Dülken, funktioniert nicht
		System.out.println(""+ p1 );
		System.out.println(" g: "+ g);
		try {
			new StatsForPOI ().simpleAlgorithm(p1, bufferSize);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
