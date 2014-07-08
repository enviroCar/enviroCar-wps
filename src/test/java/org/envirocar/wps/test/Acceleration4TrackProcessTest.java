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

import static org.junit.Assert.*;

import org.envirocar.wps.Acceleration4TrackProcess;
import org.junit.Test;


/**
 * test for mean acceleration computation per track
 * 
 * @author staschc
 *
 */
public class Acceleration4TrackProcessTest {

	@Test
	public void test() {
		
		Acceleration4TrackProcess process = new Acceleration4TrackProcess();
		try {
			double acceleration = process.getAcceleration4Track("539e0127e4b01607fa47bfd5");
			assertEquals(0.04738652564102559,acceleration,0.00002);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
