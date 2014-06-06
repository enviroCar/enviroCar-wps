/**
 * Copyright (C) 2013
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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.CRS;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

@Algorithm(version = "1.0.0")
public class DataTransformProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(DataTransformProcess.class);

	private SimpleFeatureTypeBuilder typeBuilder;

    public DataTransformProcess() {
        super();
    }
    
    private FeatureCollection result;
    private String url;

    @ComplexDataOutput(identifier = "result", binding = GTVectorDataBinding.class)
    public FeatureCollection getResult() {
        return result;
    }

    @LiteralDataInput(identifier = "url", abstrakt="Specify the URL to an EnviroCar track, e.g. https://giv-car.uni-muenster.de/dev/rest/tracks/51dcfb5ee4b0a504afb7c182")
    public void setWidth(String url) {
        this.url = url;
    }
	
    @Execute
	public void transformEnviroCarData() throws Exception {
				
		result = createFeaturesFromJSON(new URL(url));
		
		LOGGER.debug("FeatureCollection size: " + result.size());
	}	

    public SimpleFeatureCollection createFeaturesFromJSON(URL url) throws IOException{
    	
		InputStream in = url.openStream();

		ObjectMapper objMapper = new ObjectMapper();

		Map<?, ?> map = objMapper.readValue(in, Map.class);

		ArrayList<?> features = null;

		for (Object o : map.keySet()) {
			Object entry = map.get(o);

			if (o.equals("features")) {
				features = (ArrayList<?>) entry;
			}
		}

		GeometryFactory geomFactory = new GeometryFactory();

		List<SimpleFeature> simpleFeatureList = new ArrayList<SimpleFeature>();
		
		String uuid = UUID.randomUUID().toString().substring(0, 5);

		String namespace = "http://www.52north.org/" + uuid;

		SimpleFeatureType sft = null;

		SimpleFeatureBuilder sfb = null;
		
		typeBuilder = new SimpleFeatureTypeBuilder();
		try {
			typeBuilder.setCRS(CRS.decode("EPSG:4326"));
		} catch (NoSuchAuthorityCodeException e) {
			LOGGER.error("Could not decode EPSG:4326", e);
		} catch (FactoryException e) {
			LOGGER.error("Could not decode EPSG:4326", e);
		}

		typeBuilder.setNamespaceURI(namespace);
		Name nameType = new NameImpl(namespace, "Feature-" + uuid);
		typeBuilder.setName(nameType);

		typeBuilder.add("geometry", Point.class);
		typeBuilder.add("id", String.class);
		typeBuilder.add("time", String.class);

		Set<String> distinctPhenomenonNames = gatherPropertiesForFeatureTypeBuilder(features);
		
		for (Object object : features) {				
			
			if (object instanceof LinkedHashMap<?, ?>) {
				LinkedHashMap<?, ?> featureMap = (LinkedHashMap<?, ?>) object;

				Object geometryObject = featureMap.get("geometry");
				
				Point point = null;
				
				if(geometryObject instanceof LinkedHashMap<?, ?>){
					LinkedHashMap<?, ?> geometryMap = (LinkedHashMap<?, ?>)geometryObject;
					
					Object coordinatesObject = geometryMap.get("coordinates");
					
					if(coordinatesObject instanceof ArrayList<?>){
						ArrayList<?> coordinatesList = (ArrayList<?>)coordinatesObject;
						
						Object xObj = coordinatesList.get(0);
						Object yObj = coordinatesList.get(1);
						
						point = geomFactory.createPoint(new Coordinate(Double.parseDouble(xObj.toString()), Double.parseDouble(yObj.toString())));
						
					}
				}
				
				Object propertiesObject = featureMap.get("properties");

				if (propertiesObject instanceof LinkedHashMap<?, ?>) {
					LinkedHashMap<?, ?> propertiesMap = (LinkedHashMap<?, ?>) propertiesObject;	

					/*
					 * get id and time
					 */
					
					String id = propertiesMap.get("id").toString();
					String time = propertiesMap.get("time").toString();

					Object phenomenonsObject = propertiesMap.get("phenomenons");

					if (phenomenonsObject instanceof LinkedHashMap<?, ?>) {
						LinkedHashMap<?, ?> phenomenonsMap = (LinkedHashMap<?, ?>) phenomenonsObject;
						/*
						 * properties are id, time and phenomenons
						 */
						if(sft == null){
							sft = buildFeatureType(distinctPhenomenonNames);
							sfb = new SimpleFeatureBuilder(sft);
						}
						sfb.set("id", id);
						sfb.set("time", time);
						sfb.set("geometry", point);
						
						for (Object phenomenonKey : phenomenonsMap.keySet()) {

							Object phenomenonValue = phenomenonsMap
									.get(phenomenonKey);

							if (phenomenonValue instanceof LinkedHashMap<?, ?>) {
								LinkedHashMap<?, ?> phenomenonValueMap = (LinkedHashMap<?, ?>) phenomenonValue;

								String value = phenomenonValueMap.get("value")
										.toString();
								String unit = phenomenonValueMap.get("unit")
										.toString();
								
								/*
								 * create property name
								 */
								String propertyName = phenomenonKey.toString() + " (" + unit + ")";
								if(sfb != null){
									sfb.set(propertyName, value);
								}
								
							}

						}
						if(sfb != null){							
							simpleFeatureList.add(sfb.buildFeature(id));
						}
					}
				}

			}
		}
		
		return new ListFeatureCollection(sft, simpleFeatureList);    	
    }
    
	private SimpleFeatureType buildFeatureType(Set<String> properties) {

		for (String phenomenonKey : properties) {			
				typeBuilder.add(phenomenonKey,
						String.class);
		}
		return typeBuilder.buildFeatureType();
	}

	private Set<String> gatherPropertiesForFeatureTypeBuilder(ArrayList<?> features) {
		Set<String> distinctPhenomenonNames = new HashSet<String>();

		for (Object object : features) {

			if (object instanceof LinkedHashMap<?, ?>) {
				LinkedHashMap<?, ?> featureMap = (LinkedHashMap<?, ?>) object;

				Object propertiesObject = featureMap.get("properties");

				if (propertiesObject instanceof LinkedHashMap<?, ?>) {
					LinkedHashMap<?, ?> propertiesMap = (LinkedHashMap<?, ?>) propertiesObject;

					Object phenomenonsObject = propertiesMap.get("phenomenons");

					if (phenomenonsObject instanceof LinkedHashMap<?, ?>) {
						LinkedHashMap<?, ?> phenomenonsMap = (LinkedHashMap<?, ?>) phenomenonsObject;

						for (Object phenomenonKey : phenomenonsMap.keySet()) {

							Object phenomenonValue = phenomenonsMap
									.get(phenomenonKey);

							if (phenomenonValue instanceof LinkedHashMap<?, ?>) {
								LinkedHashMap<?, ?> phenomenonValueMap = (LinkedHashMap<?, ?>) phenomenonValue;

								String unit = phenomenonValueMap.get("unit")
										.toString();

								distinctPhenomenonNames.add(phenomenonKey
										.toString() + " (" + unit + ")");
							}

						}
					}

				}
			}
		}
		return distinctPhenomenonNames;
	}	

}