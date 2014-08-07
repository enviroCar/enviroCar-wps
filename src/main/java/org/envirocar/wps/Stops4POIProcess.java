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

import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.envirocar.wps.util.EnviroCarFeatureParser;
import org.envirocar.wps.util.EnviroCarWpsConstants;
import org.envirocar.wps.util.EnviroCarWpsConstants.FeatureProperties;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.n52.wps.algorithm.annotation.Algorithm;
import org.n52.wps.algorithm.annotation.ComplexDataInput;
import org.n52.wps.algorithm.annotation.ComplexDataOutput;
import org.n52.wps.algorithm.annotation.Execute;
import org.n52.wps.algorithm.annotation.LiteralDataInput;
import org.n52.wps.io.data.binding.complex.GTVectorDataBinding;
import org.n52.wps.io.data.binding.complex.JTSGeometryBinding;
import org.n52.wps.server.AbstractAnnotatedAlgorithm;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.geometry.MismatchedDimensionException;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Computes the stops of envirocar tracks at points of interest
 * 
 * @author Christoph Stasch, Benjamin Pross
 *
 */
@Algorithm(version = "1.0.0")
public class Stops4POIProcess extends AbstractAnnotatedAlgorithm {

    private static Logger LOGGER = LoggerFactory.getLogger(Stops4POIProcess.class);
    
   
    private static final int EPSG_CODE_GPS = 4326;
    private static final int EPSG_CODE_GK3 = 31467;

	/**
	 * constructor
	 */
    public Stops4POIProcess() {
        super();
    }
    
    private FeatureCollection result;
    private double bufferSize;
    private double maxSpeed;
    private Geometry pointOfInterest;

    @ComplexDataOutput(identifier = "result", binding = GTVectorDataBinding.class)
    public FeatureCollection getResult() {
        return result;
    }

    @LiteralDataInput(identifier = "bufferSize", abstrakt="Specify the size of the buffer that is used to identify stops around a traffic light.")
    public void setBufferSize(double bufferSize) {
        this.bufferSize = bufferSize;
    }
    
    @LiteralDataInput(identifier = "maxSpeed", abstrakt="Maximum speed value that identifies measurement as stop.")
    public void setMaxSpeed(double maxSpeed) {
        this.maxSpeed = maxSpeed;
    }
    
    @ComplexDataInput(identifier = "pointOfInterest", binding = JTSGeometryBinding.class)
    public void setData(Geometry data) {
        this.pointOfInterest = data;
    }
	
    @Execute
	public void getStops4TrafficLights() throws Exception {
    	this.result = getStops4PointOfInterests(this.pointOfInterest,this.bufferSize,this.maxSpeed);
	}	
    
    
    /**
     * computes stops at one or more points
     * 
     * @param tlPoint
     * @param bufferSizeP
     * @param maxSpeedP
     * @return
     * @throws Exception
     */
    public SimpleFeatureCollection getStops4PointOfInterests(Geometry tlPoint, double bufferSizeP, double maxSpeedP) throws Exception{
    	
		//set up feature type for features that are returned
		String uuid = UUID.randomUUID().toString().substring(0, 5);
		String namespace = "http://www.52north.org/" + uuid;
		SimpleFeatureType sft = null;
		SimpleFeatureBuilder sfb = null;
		SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
		typeBuilder.setCRS(CRS.decode("EPSG:" + EPSG_CODE_GPS));
		typeBuilder.setNamespaceURI(namespace);
		Name nameType = new NameImpl(namespace, "Feature-" + uuid);
		typeBuilder.setName(nameType);
		typeBuilder.add("geometry", Point.class);
		typeBuilder.add("id", String.class);
		typeBuilder.add("totalNumberOfStops", Integer.class);
		typeBuilder.add("percentageOfStops", Double.class);
		typeBuilder.add("totalNumberOfTracks", Integer.class);
		sft = typeBuilder.buildFeatureType();
		sfb = new SimpleFeatureBuilder(sft);
		
		//create feature collection that is returned
		List<SimpleFeature> simpleFeatureList = new ArrayList<SimpleFeature>();
		
		if(tlPoint instanceof Point){		
    	    Point inputPoint = (Point)tlPoint;
    	    LOGGER.debug("Computing stops for point ("+inputPoint.getX()+", "+inputPoint.getY()+") with buffer size "+bufferSizeP+" m and max speed value "+ maxSpeedP);
    	    simpleFeatureList.add(getStops4PointOfInterest(inputPoint, bufferSizeP, maxSpeedP, sfb));    	
		}else if(tlPoint instanceof MultiPoint){
			
			MultiPoint inputPoints = (MultiPoint)tlPoint;
			
			for (int i = 0; i < inputPoints.getNumGeometries(); i++) {			
				Point inputPoint = (Point) inputPoints.getGeometryN(i);				
	    	    LOGGER.debug("Computing stops for point ("+inputPoint.getX()+", "+inputPoint.getY()+") with buffer size "+bufferSizeP+" m and max speed value "+ maxSpeedP);
	    	    simpleFeatureList.add(getStops4PointOfInterest(inputPoint, bufferSizeP, maxSpeedP, sfb));
			}			
		}			
		return new ListFeatureCollection(sft, simpleFeatureList);
    }
    
    /**
     * computes stops at a point
     * 
     * @param tlPoint
     * @param bufferSizeP
     * @param maxSpeedP
     * @return
     * @throws Exception
     */
    public SimpleFeature getStops4PointOfInterest(Geometry tlPoint, double bufferSizeP, double maxSpeedP, SimpleFeatureBuilder sfb) throws Exception{
    	
    	Point inputPoint = (Point)tlPoint;
    	LOGGER.debug("Computing stops for point ("+inputPoint.getX()+", "+inputPoint.getY()+") with buffer size "+bufferSizeP+" m and max speed value "+ maxSpeedP);
    	tlPoint.setSRID(EPSG_CODE_GPS);
    	
    	//project to GK3 to compute buffer in meters
    	Geometry gkPoint = projectToGK3(tlPoint);
    	Geometry buffer = gkPoint.buffer(bufferSizeP);
    	
    	//re-transform to WGS84 coordinates for BBOX filter in query of tracks from Envirocar server
    	Geometry envelope = transformToWGS(buffer.getEnvelope());
    	Coordinate[] coords = envelope.getCoordinates();
    	double minx = coords[0].x;
    	double maxx = coords[2].x;
    	double miny = coords[0].y;
    	double maxy = coords[2].y;
    	
    	//query tracks from EnviroCar server
    	String bboxQuery = "?bbox="+minx+","+miny+","+maxx+","+maxy;
		String queryUrl = EnviroCarWpsConstants.ENV_SERVER_URL+"/tracks"+bboxQuery;
    	URL u = new URL(queryUrl);
        InputStream in = u.openStream();
		ObjectMapper objMapper = new ObjectMapper();
		Map<?, ?> map = objMapper.readValue(in, Map.class);
		ArrayList<?> trackIDs = null;

		//init result properties
		int numberOfTracks = 0, numberOfStops=0;
		double percentage = 0.0;
		
		//create spatial filter for checking whether measurements of tracks are within buffer around POI
		FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();
		GeometryFactory geomFac = new GeometryFactory();
		Polygon bufferFlipped = geomFac.createPolygon(geomFac.createLinearRing(envelope.getCoordinates()),null);
		Filter filter = ff.within(ff.property(FeatureProperties.GEOMETRY),ff.literal(bufferFlipped));
		
		//iterate over track IDs
		for (Object o : map.keySet()) {
			Object entry = map.get(o);
			if (o.equals("tracks")) {
				trackIDs = (ArrayList<?>) entry;
				numberOfTracks = trackIDs.size();
				
				//for each track query the measurements  
				for (Object item:trackIDs){
					String trackID = (String) ((LinkedHashMap)item).get("id");
					LOGGER.debug("Getting features for track with ID: " + trackID);
					URL trackUrl = new URL(EnviroCarWpsConstants.ENV_SERVER_URL+"/tracks/"+trackID+"/measurements"+bboxQuery);
					EnviroCarFeatureParser parser = new EnviroCarFeatureParser();
					SimpleFeatureCollection trackFc = parser.createFeaturesFromJSON(trackUrl);
					SimpleFeatureIterator featIter = trackFc.features();
					try {
						while (featIter.hasNext()){
							SimpleFeature feat = featIter.next();
							double speed;
							if (feat.getAttribute("Speed (km/h)")!=null){
								speed = Double.parseDouble((String)feat.getAttribute("Speed (km/h)"));
								if (speed<maxSpeedP){
									numberOfStops++;
									break; //TODO currently, if one stop is contained, the loop breaks!
								}
							}
						}
					} catch (Exception e){
						LOGGER.debug("Error while extracting speed attribute from features: "+e.getLocalizedMessage());
						throw(e);
					}
					finally{
						featIter.close();
					}
				}
			}
		}
		
		//compute percentage of stops
		percentage = ((double) numberOfStops)/((double)numberOfTracks)*100;
		
		String featID = "feature-"+UUID.randomUUID().toString().substring(0, 5);
				
		//set feature properties
		sfb.set("geometry",tlPoint);
		sfb.set("id", featID);
		sfb.set("totalNumberOfStops", numberOfStops);
		sfb.set("percentageOfStops", percentage);
		sfb.set("totalNumberOfTracks", numberOfTracks);
		LOGGER.debug("Number of stops: " + numberOfStops + "; total number of tracks: "+ numberOfTracks + "; percentage: "+ percentage);

		return sfb.buildFeature(featID);
    }
    
    /**
     * helper method for projecting from WGS84 to Gauss-Krueger-3 projection
     * 
     * @param geom
     * 			geometry that should be projected
     * @return 
     * 			projected geometry
     * @throws Exception
     * 			if projection fails
     */
    private static Geometry projectToGK3(Geometry geom) throws Exception{
    	Geometry result = null;
    	Coordinate[] coordsNew = geom.getCoordinates();
    	if (geom instanceof Point && coordsNew.length==1){
    		geom = new GeometryFactory().createPoint(coordsNew[0]);
    	}
    	CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:"+EPSG_CODE_GPS);
    	CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:"+EPSG_CODE_GK3);
    	MathTransform trans = CRS.findMathTransform(sourceCRS, targetCRS);
    	result = JTS.transform(geom,trans);
    	return result;
    }
    
    /**
     * helper method for transformation from Gauss-Krueger-3 projection to WGS84
     * 
     * @param geom
     * 			geometry that should be projected
     * @return 
     * 			projected geometry
     * @throws Exception
     * 			if projection fails
     */
    private static Geometry transformToWGS(Geometry geom) throws NoSuchAuthorityCodeException, FactoryException, MismatchedDimensionException, TransformException{
    	Geometry result = null;
    	CoordinateReferenceSystem sourceCRS = CRS.decode("EPSG:"+EPSG_CODE_GK3);
    	CoordinateReferenceSystem targetCRS = CRS.decode("EPSG:"+EPSG_CODE_GPS);
    	MathTransform trans = CRS.findMathTransform(sourceCRS, targetCRS);
    	result = JTS.transform(geom,trans);
    	if (result instanceof Polygon){
    		Polygon poly = (Polygon)result;
    		Coordinate[] coords = poly.getExteriorRing().getCoordinates();
    		GeometryFactory geomFactory = new GeometryFactory();
    		result  = geomFactory.createPolygon(geomFactory.createLinearRing(coords),null);
    	}
    	return result;
    }

}