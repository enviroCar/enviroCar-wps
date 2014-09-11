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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.algorithm.annotation.LiteralDataOutput;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Algorithm(version = "1.0.0")
public class GetFuelPriceProcess extends AbstractAnnotatedAlgorithm {
	
	private static Logger LOGGER = LoggerFactory
			.getLogger(GetFuelPriceProcess.class);
	private static ReadWriteLock readWriteLock = new ReentrantReadWriteLock();
	
	private String fuelType;
	private double fuelPrice = 0.0D;
	private File fuelPriceStorage = new File(
			System.getProperty("java.io.tmpdir") + File.separator
					+ "wpsFuelPriceStorage.txt");
	
	public GetFuelPriceProcess() {
		if (!fuelPriceStorage.exists()) {
			try {
				InputStream archive = getClass().getResourceAsStream("/fuelPriceArchive");
				fuelPriceStorage.createNewFile();
				Files.copy(archive, fuelPriceStorage.toPath(), StandardCopyOption.REPLACE_EXISTING);
			} catch (IOException e) {
				LOGGER.warn(e.getMessage(), e);
			}
		}
	}

	@LiteralDataOutput(identifier = "fuelPrice")
	public double getFuelPrice() {
		return this.fuelPrice;
	}

	@LiteralDataInput(identifier = "fuelType", minOccurs = 1)
	public void setFuelType(String fuelType) {
		this.fuelType = fuelType;
	}

	@Execute
	public void executeGetFuelPrice() {
		if (this.fuelPriceStorage.exists()) {
			try {
				String content = "";
				String line = "";
				String lastLine = "";
				readWriteLock.readLock().lock();

				BufferedReader bufferedReader = null;
				try {
					bufferedReader = new BufferedReader(new FileReader(
							this.fuelPriceStorage));

					while ((line = bufferedReader.readLine()) != null) {
						content = content.concat(line + "\n");
						lastLine = line;
					}
				} catch (Exception e) {
					LOGGER.error(e.getMessage(), e);
					return;
				} finally {
					bufferedReader.close();
					readWriteLock.readLock().unlock();
				}

				String[] fuelPricesArray = lastLine.split(";");

				this.fuelPrice = getFuelPriceFromArray(fuelPricesArray);
			} catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
				return;
			}
		} else
			try {
				LOGGER.info("No fuel prices in storage, requesting new ones.");

				String content = getLatestFuelPrice();

				String[] fuelPricesArray = content.split(";");

				this.fuelPrice = getFuelPriceFromArray(fuelPricesArray);

				writeFuelPriceToStorage(content);
			} catch (Exception e) {
				LOGGER.warn(e.getMessage(), e);
			}
	}

	private void writeFuelPriceToStorage(String content) {
		LOGGER.info("Writing new fuel prices to storage");

		readWriteLock.writeLock().lock();

		LOGGER.info("Write locked.");
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					this.fuelPriceStorage));

			writer.write(content);

			writer.close();
		} catch (Exception e) {
			LOGGER.error(e.getMessage());
		} finally {
			readWriteLock.writeLock().unlock();

			LOGGER.info("Write unlocked.");
		}
	}

	private double getFuelPriceFromArray(String[] fuelPricesArray) {
		if (this.fuelType.equals("gasoline"))
			return Double.parseDouble(fuelPricesArray[2]);
		if (this.fuelType.equals("diesel")) {
			return Double.parseDouble(fuelPricesArray[5]);
		}
		return 0.0D;
	}

	private String getLatestFuelPrice() throws Exception {
		LOGGER.info("Requesting latest fuel price");

		URL url = new URL(
				"http://export.benzinpreis-aktuell.de/exportdata.txt?code=69T36ft7QDY70L4");

		BufferedReader reader = new BufferedReader(new InputStreamReader(
				url.openStream()));

		String content = "";
		String line = "";

		while ((line = reader.readLine()) != null) {
			content = content.concat(line);
		}

		LOGGER.info("Done: " + content);

		return content;
	}
}