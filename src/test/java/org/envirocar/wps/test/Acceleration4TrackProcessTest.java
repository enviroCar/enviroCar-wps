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
			assertEquals(acceleration,4.677647008547006E-8,0.00002);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

}
