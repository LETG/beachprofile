package fr.indigeo.wps.bpt.tools;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.referencing.GeodeticCalculator;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeType;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.TransformException;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.operation.distance.DistanceOp;
import org.locationtech.jts.linearref.LengthIndexedLine;

public class BeachProfileTrackingTools {

	private static final Logger LOGGER = LogManager.getLogger(BeachProfileTrackingTools.class);
	private static final String START_DISTANCE_FIELD = "startdistance";
	private static final String END_DISTANCE_FIELD = "enddistance";

	public BeachProfileTrackingTools() {}

	private static class ProfileLine {
		Date date;
		LineString line;
		double startDistance;
		double endDistance;

		ProfileLine(Date date, LineString line, double startDistance, double endDistance) {
			this.date = date;
			this.line = line;
			this.startDistance = startDistance;
			this.endDistance = endDistance;
		}
	}

	private static class InterpolatedProfile {
		LineString line;
		double startDistance;
		double endDistance;

		InterpolatedProfile(LineString line, double startDistance, double endDistance) {
			this.line = line;
			this.startDistance = startDistance;
			this.endDistance = endDistance;
		}
	}

	public FeatureCollection<SimpleFeatureType, SimpleFeature> reprojectFeatureCollectionToRefLine(FeatureCollection<SimpleFeatureType, SimpleFeature> fc, FeatureCollection<SimpleFeatureType, SimpleFeature> refline, double distanceMax) {
		
		LOGGER.debug("reprojectFeatureCollectionToRefLine");
		if (fc == null || refline == null) {
			return fc;
		}

		Geometry refGeometry = buildRefGeometry(refline);
		if (refGeometry == null) {
			return fc;
		}

		double effectiveDistanceMax = distanceMax > 0 ? distanceMax : 20d;
		boolean filterByDistance = effectiveDistanceMax > 0;
		DefaultFeatureCollection reprojected = new DefaultFeatureCollection(null, fc.getSchema());
		FeatureIterator<SimpleFeature> iterator = fc.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Object geometry = feature.getDefaultGeometry();
					if (!(geometry instanceof LineString)) {
						reprojected.add(feature);
						continue;
					}

					LineString line = (LineString) geometry;
					Coordinate[] coords = line.getCoordinates();
					List<Coordinate> projected = new ArrayList<>();
					for (int i = 0; i < coords.length; i++) {
						Coordinate c = coords[i];
						org.locationtech.jts.geom.Point point = line.getFactory().createPoint(c);
						double distanceToRef = refGeometry.distance(point);
						if (filterByDistance && distanceToRef > effectiveDistanceMax) {
							LOGGER.info("Point ignoré car distance {} > distanceMax {} (profil {}, point {})", distanceToRef, effectiveDistanceMax, resolveProfileDate(feature), resolvePointIdentifier(feature, i));
							continue;
						}
						Coordinate target = DistanceOp.nearestPoints(refGeometry, point)[0];
						projected.add(new Coordinate(target.x, target.y, c.getZ()));
				}

				int geomIndex = feature.getFeatureType().indexOf(feature.getDefaultGeometryProperty().getName());
				SimpleFeatureBuilder builder = new SimpleFeatureBuilder(feature.getFeatureType());
				Coordinate[] projectedArray = projected.toArray(new Coordinate[0]);
				if (projectedArray.length == 1) {
					LOGGER.info("Seulement un point conservé après filtrage distanceMax, duplication pour conserver un LineString (profil {}, point {})", resolveProfileDate(feature), resolvePointIdentifier(feature, 0));
					projectedArray = new Coordinate[] { projectedArray[0], projectedArray[0] };
				}
				for (int i = 0; i < feature.getAttributeCount(); i++) {
					if (i == geomIndex) {
						builder.add(line.getFactory().createLineString(projectedArray));
					} else {
						builder.add(feature.getAttribute(i));
					}
				}
				reprojected.add(builder.buildFeature(feature.getID()));
			}
		} finally {
			iterator.close();
		}
		return reprojected;
	}

	public FeatureCollection<SimpleFeatureType, SimpleFeature> reprojectFeatureCollectionToRefLine(FeatureCollection<SimpleFeatureType, SimpleFeature> fc, FeatureCollection<SimpleFeatureType, SimpleFeature> refline) {
		return reprojectFeatureCollectionToRefLine(fc, refline, 20d);
	}

	private String resolveProfileDate(SimpleFeature feature) {
		Object creationDate = feature.getAttribute("creationdate");
		if (creationDate == null) {
			creationDate = feature.getAttribute("date");
		}
		if (creationDate != null) {
			return creationDate.toString();
		}
		return feature.getID();
	}

	private String resolvePointIdentifier(SimpleFeature feature, int coordinateIndex) {
		Object ogcFid = feature.getAttribute("ogc_fid");
		String featureId = ogcFid != null ? ogcFid.toString() : feature.getID();
		return featureId + "#" + (coordinateIndex + 1);
	}

	private Geometry buildRefGeometry(FeatureCollection<SimpleFeatureType, SimpleFeature> refline) {
		List<LineString> refLines = new ArrayList<LineString>();
		FeatureIterator<SimpleFeature> iterator = refline.features();
		try {
			while (iterator.hasNext()) {
				Object geometry = iterator.next().getDefaultGeometry();
				if (geometry instanceof LineString) {
					refLines.add((LineString) geometry);
				}
			}
		} finally {
			iterator.close();
		}
		if (refLines.isEmpty()) {
			return null;
		}
		GeometryFactory geometryFactory = refLines.get(0).getFactory();
		return geometryFactory.createMultiLineString(refLines.toArray(new LineString[0]));
	}

	public LineString interpolateLineStringConstantStep(LineString line, double step, CoordinateReferenceSystem crs) {

		GeometryFactory gf = new GeometryFactory();
		Coordinate[] coords = line.getCoordinates();

		List<Coordinate> result = new ArrayList<>();
		result.add(coords[0]); // on part de la tête du profil

		GeodeticCalculator gc = new GeodeticCalculator(crs);

		double cumulative = 0.0;
		double target = step;

		for (int i = 1; i < coords.length; i++) {

			// Distance du segment i-1 → i
			try {
				gc.setStartingPosition(JTS.toDirectPosition(coords[i - 1], crs));
				gc.setDestinationPosition(JTS.toDirectPosition(coords[i], crs));
			} catch (TransformException e) {
				
				LOGGER.error("Erreur durant l'interpolation",e);
				continue; 
			}
			double segmentLength = gc.getOrthodromicDistance();

			// Tant que le point recherché cible se trouve dans ce segment :
			while (cumulative + segmentLength >= target) {
				double ratio = (target - cumulative) / segmentLength;
				double x = coords[i - 1].x + ratio * (coords[i].x - coords[i - 1].x);
				double y = coords[i - 1].y + ratio * (coords[i].y - coords[i - 1].y);

				result.add(new Coordinate(x, y));

				target += step; 
			}

			cumulative += segmentLength;
		}

		return gf.createLineString(result.toArray(new Coordinate[0]));
	}
	
	/**
	 * Do an interpolation for each Feature's Geometry of a FeatureCollection with an interval
	 * @param fc
	 * @param interval in meters
	 * @return
	 */
	public FeatureCollection<SimpleFeatureType, SimpleFeature> InterpolateFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> fc, FeatureCollection<SimpleFeatureType, SimpleFeature> refline, double interval){
		LOGGER.debug("InterpolateFeatureCollection");
		if(interval <= 0 || refline == null){
			return fc;
		}
		// load the LineStrings
		// With geoserver 2.21.5 version CRS give WGS84 instead of 2154
		CoordinateReferenceSystem myCrs;
		try {
			myCrs = CRS.decode("EPSG:2154"); // fc.getSchema().getCoordinateReferenceSystem();
		
			Geometry refGeometry = buildRefGeometry(refline);
			if (refGeometry == null) {
				return fc;
			}
			LengthIndexedLine refIndexed = new LengthIndexedLine(refGeometry);
			double refLength = BeachProfileUtils.getDistanceFromCoordinates(refGeometry.getCoordinates(), myCrs);
			List<Double> targetDistances = buildTargetDistances(refLength, interval);

			DefaultFeatureCollection resultFeatureCollection = null;
			// get Linestrings order by date
			Map<Date, LineString> lineStrings = BeachProfileUtils.getProfilesFromFeature(fc);
			Map<Date, InterpolatedProfile> interpolatedLineStrings = new HashMap<Date, InterpolatedProfile>();
			
			// do the interpolation aligned on the reference line
			lineStrings.forEach((id,line) -> {
				InterpolatedProfile interpolated = interpolateLineOnReference(line, refIndexed, targetDistances, interval, myCrs);
				interpolatedLineStrings.put(id, interpolated);
			});
			
			//create a new FeatureCollection to add the new coordinates
			SimpleFeatureTypeBuilder simpleFeatureTypeBuilder = new SimpleFeatureTypeBuilder();
			simpleFeatureTypeBuilder.setCRS(myCrs);
			simpleFeatureTypeBuilder.setName("featureType");
			simpleFeatureTypeBuilder.add("geometry", LineString.class);
			simpleFeatureTypeBuilder.add("date", String.class);
			simpleFeatureTypeBuilder.add(START_DISTANCE_FIELD, Double.class);
			simpleFeatureTypeBuilder.add(END_DISTANCE_FIELD, Double.class);

			
			// init DefaultFeatureCollection
			SimpleFeatureBuilder simpleFeatureBuilder = new SimpleFeatureBuilder(simpleFeatureTypeBuilder.buildFeatureType());
			resultFeatureCollection = new DefaultFeatureCollection(null, simpleFeatureBuilder.getFeatureType());
			// add geometrie to defaultFeatures
			for (Entry<Date, InterpolatedProfile> entry : interpolatedLineStrings.entrySet())
			{
				InterpolatedProfile profile = entry.getValue();
				simpleFeatureBuilder.add(profile.line);
				simpleFeatureBuilder.add(entry.getKey());
				simpleFeatureBuilder.add(profile.startDistance);
				simpleFeatureBuilder.add(profile.endDistance);
				resultFeatureCollection.add(simpleFeatureBuilder.buildFeature(entry.getKey() + ""));
			}
			
			return resultFeatureCollection;
		} 
		catch (FactoryException e) {
			LOGGER.debug("FactoryException",e);
						
			return null;
		}
	}

	private List<Double> buildTargetDistances(double refLength, double interval) {
		List<Double> targetDistances = new ArrayList<>();
		double current = 0d;
		while (current <= refLength) {
			targetDistances.add(current);
			current += interval;
		}
		if (targetDistances.get(targetDistances.size() - 1) < refLength) {
			targetDistances.add(refLength);
		}
		return targetDistances;
	}

	private InterpolatedProfile interpolateLineOnReference(LineString line, LengthIndexedLine refIndexed, List<Double> targetDistances, double interval, CoordinateReferenceSystem crs) {
		if (line == null || refIndexed == null || targetDistances == null || targetDistances.isEmpty()) {
			return new InterpolatedProfile(line, 0d, 0d);
		}

		double lineLength = BeachProfileUtils.getDistanceFromCoordinates(line.getCoordinates(), crs);
		if (lineLength == 0) {
			return new InterpolatedProfile(line, 0d, 0d);
		}

		Coordinate start = line.getCoordinateN(0);
		double startOnRef = refIndexed.indexOf(start);
		double endOnRef = refIndexed.indexOf(line.getCoordinateN(line.getNumPoints() - 1));
		double minRef = Math.min(startOnRef, endOnRef);
		double maxRef = Math.max(startOnRef, endOnRef);
		boolean sameDirection = startOnRef <= endOnRef;

		List<Coordinate> interpolated = new ArrayList<>();
		interpolated.add(sameDirection ? line.getCoordinateN(0) : line.getCoordinateN(line.getNumPoints() - 1));
		for (double targetDistance : targetDistances) {
			if (targetDistance <= minRef) {
				continue; // line has not started yet on the refline
			}
			if (targetDistance >= maxRef) {
				break; // past the end of this line, remaining targets are beyond
			}
			double targetOnLine = sameDirection ? targetDistance - startOnRef : startOnRef - targetDistance;
			Coordinate c = interpolateCoordinateAtDistance(line.getCoordinates(), targetOnLine, crs);
			if (c != null) {
				interpolated.add(c);
			}
		}
		interpolated.add(sameDirection ? line.getCoordinateN(line.getNumPoints() - 1) : line.getCoordinateN(0));
		return new InterpolatedProfile(line.getFactory().createLineString(interpolated.toArray(new Coordinate[0])), minRef, maxRef);
	}

	private Coordinate interpolateCoordinateAtDistance(Coordinate[] coords, double targetDistance, CoordinateReferenceSystem crs) {
		if (coords == null || coords.length == 0 || crs == null) {
			return null;
		}
		if (targetDistance <= 0) {
			return new Coordinate(coords[0].x, coords[0].y, coords[0].z);
		}

		double cumulative = 0d;
		GeodeticCalculator gc = new GeodeticCalculator(crs);
		for (int i = 1; i < coords.length; i++) {
			try {
				gc.setStartingPosition(JTS.toDirectPosition(coords[i - 1], crs));
				gc.setDestinationPosition(JTS.toDirectPosition(coords[i], crs));
			} catch (TransformException e) {
				LOGGER.error("Erreur durant l'interpolation", e);
				continue;
			}
			double segmentLength = gc.getOrthodromicDistance();
			if (cumulative + segmentLength + 0.0001 >= targetDistance) {
				double ratio = (targetDistance - cumulative) / segmentLength;
				double x = coords[i - 1].x + ratio * (coords[i].x - coords[i - 1].x);
				double y = coords[i - 1].y + ratio * (coords[i].y - coords[i - 1].y);
				double z = coords[i - 1].z + ratio * (coords[i].z - coords[i - 1].z);
				return new Coordinate(x, y, z);
			}
			cumulative += segmentLength;
		}
		return new Coordinate(coords[coords.length - 1].x, coords[coords.length - 1].y, coords[coords.length - 1].z);
	}
	
	/**
	 * Calculate the area of sediments for a length and compare it between each Feature
	 * @param profile
	 * @param useSmallestDistance
	 * @param minDist
	 * @param maxDist
	 * @return
	 */
	public FeatureCollection<SimpleFeatureType, SimpleFeature> sedimentaryBalanceCalc(FeatureCollection<SimpleFeatureType, SimpleFeature> profile, boolean useSmallestDistance, double minDist, double maxDist) {
		// load the LineStrings
		// With geoserver 2.21.5 version CRS give WGS84 instead of 2154
		CoordinateReferenceSystem myCrs;
		try {
			myCrs = CRS.decode("EPSG:2154"); // fc.getSchema().getCoordinateReferenceSystem();
		
			//create a new FeatureCollection to write the calculation results
			SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
			b.setName("featureType");
			b.add("date", String.class);
			b.add("volume", Double.class);
			b.add("diffWithPrevious", Double.class);
			b.add("totalEvolution", Double.class);
			SimpleFeatureType type = b.buildFeatureType();
			SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);		
			DefaultFeatureCollection dfc = new DefaultFeatureCollection();
			
			List<ProfileLine> refProfile = getProfileLinesFromFeature(profile, myCrs);
			double lastProfileArea = 0;
			double tempProfileArea = 0;
			double totalEvolution = 0;
			double effectiveMinDist = minDist;
			double effectiveMaxDist = maxDist;
			boolean hasReferenceProfile = false;

			if(refProfile.isEmpty()) {
				return dfc;
			}

			ProfileLine firstProfile = refProfile.get(0);
			if(useSmallestDistance) {
				effectiveMinDist = Math.max(effectiveMinDist, refProfile.stream().mapToDouble(p -> p.startDistance).max().orElse(effectiveMinDist));
				if(effectiveMaxDist <= 0) {
					effectiveMaxDist = refProfile.stream().mapToDouble(p -> p.endDistance).min().orElse(firstProfile.endDistance);
				}
				else {
					effectiveMaxDist = Math.min(effectiveMaxDist, refProfile.stream().mapToDouble(p -> p.endDistance).min().orElse(effectiveMaxDist));
				}
			}
			else if(effectiveMaxDist <= 0) {
				effectiveMinDist = Math.max(effectiveMinDist, firstProfile.startDistance);
				effectiveMaxDist = firstProfile.endDistance;
			}

			// For each profile 
			for (ProfileLine entry : refProfile) {
				LOGGER.debug("Calulation for date {}", entry.date.toString());
				if(entry.startDistance > effectiveMinDist + 0.001 || entry.endDistance < effectiveMaxDist - 0.001) {
					LOGGER.debug("{} | [{}, {}] | profile does not cover calculation interval [{}, {}]", entry.date.toString(), entry.startDistance, entry.endDistance, effectiveMinDist, effectiveMaxDist);
					continue;
				}

				double localMinDist = Math.max(0d, effectiveMinDist - entry.startDistance);
				double localMaxDist = effectiveMaxDist - entry.startDistance;
				if(localMinDist >= localMaxDist) {
					LOGGER.debug("{} | invalid local calculation interval [{}, {}]", entry.date.toString(), localMinDist, localMaxDist);
					continue;
				}

				if(!hasReferenceProfile){
					lastProfileArea = BeachProfileUtils.getProfileArea(entry.line.getCoordinates(), localMinDist, localMaxDist, myCrs);
					//write the result. For the first date we don't have evolutions values so we add a 0 value
					builder.add(entry.date.toString());
					builder.add(lastProfileArea);
					builder.add(0);
					builder.add(0);
					SimpleFeature sf = builder.buildFeature(null);
					dfc.add(sf);
					hasReferenceProfile = true;
				}
				else{
					tempProfileArea = BeachProfileUtils.getProfileArea(entry.line.getCoordinates(), localMinDist, localMaxDist, myCrs);
					totalEvolution += (tempProfileArea - lastProfileArea);
					//write the results
					builder.add(entry.date.toString());
					builder.add(tempProfileArea);
					builder.add((tempProfileArea - lastProfileArea));
					builder.add(totalEvolution);
					SimpleFeature sf = builder.buildFeature(null);
					dfc.add(sf);
					lastProfileArea = tempProfileArea;
				}
			}
			
			return dfc;
		} catch (FactoryException e1) {
			LOGGER.error("FactoryException",e1);	
			return null;
		}
	}

	private List<ProfileLine> getProfileLinesFromFeature(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection, CoordinateReferenceSystem crs) {
		List<ProfileLine> profileLines = new ArrayList<ProfileLine>();
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		try {
			while (iterator.hasNext()) {
				SimpleFeature feature = iterator.next();
				Geometry geometry = (Geometry) feature.getDefaultGeometry();
				if (!(geometry instanceof LineString)) {
					continue;
				}

				Date date = resolveDate(feature);
				if (date == null) {
					continue;
				}

				LineString line = (LineString) geometry;
				double startDistance = readDoubleAttribute(feature, START_DISTANCE_FIELD, 0d);
				double endDistance = readDoubleAttribute(feature, END_DISTANCE_FIELD, startDistance + BeachProfileUtils.getDistanceFromCoordinates(line.getCoordinates(), crs));
				profileLines.add(new ProfileLine(date, line, startDistance, endDistance));
			}
		} finally {
			iterator.close();
		}
		profileLines.sort((left, right) -> left.date.compareTo(right.date));
		return profileLines;
	}

	private Date resolveDate(SimpleFeature feature) {
		Collection<Property> properties = feature.getProperties();
		for (Property property : properties){
			if ( property.getValue() instanceof Date){
				return (Date) property.getValue();
			}
			try {
				return new SimpleDateFormat("yyyy-MM-dd").parse(property.getValue().toString());
			} catch (ParseException e) {
				// Keep looking for a parseable date.
			}
		}
		return null;
	}

	private double readDoubleAttribute(SimpleFeature feature, String name, double defaultValue) {
		Object value = feature.getAttribute(name);
		if (value instanceof Number) {
			return ((Number) value).doubleValue();
		}
		if (value != null) {
			try {
				return Double.parseDouble(value.toString());
			} catch (NumberFormatException e) {
				LOGGER.debug("Unable to parse {} as double", name, e);
			}
		}
		return defaultValue;
	}

	/**
	 * Convert the FeatureColleciton to a string which is then readable in .csv format
	 * @param featureCollection
	 * @return
	 */
	public String featureToCSV(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		String csvString = "";
		
		//get column name from the features properties
		List<AttributeType> attributes = featureCollection.getSchema().getTypes();
		for(AttributeType att : attributes) csvString += att.getName() + ";";
		csvString +="\n";
		
		//loop in the featureCollection, create a new line for each feature and add recovered data
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		while (iterator.hasNext()) {
			SimpleFeature feature = iterator.next();			
			for(int i = 0; i< feature.getAttributeCount(); i++)	csvString += feature.getAttribute(i) + ";";
			csvString += "\n";
		}
		
		return csvString;
	}
	
	/**
	 * Convert the FeatureColleciton to a json string object
	 * @param featureCollection
	 * @return
	 */
	public String featureToJSON(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		
		JSONObject result = new JSONObject();
		JSONArray resultsArray = new JSONArray();
		final String dateKey = "date";
		
		//loop in the featureCollection, create a new line for each feature and add recovered data
		FeatureIterator<SimpleFeature> iterator = featureCollection.features();
		while (iterator.hasNext()) {
			JSONObject bpf = new JSONObject();
			JSONArray bpfValues = new JSONArray();
			SimpleFeature feature = iterator.next();

			if(LOGGER.isDebugEnabled()){
				Collection<Property> properties = feature.getProperties();
				for (Property property : properties){
					LOGGER.debug("key : {},  value : {} " , property.getName(), property.getValue().toString());
				}
			}

			if(feature.getProperty("error") != null){
				result.put("result", "error");
				result.put("additional", feature.getProperty("error").getValue());
				return result.toString();
			}
			bpf.put("date", feature.getProperty(dateKey).getValue().toString());

			for (Property property : feature.getProperties()) {
				if(!dateKey.equals(property.getName().toString())){
					JSONObject bpfValue = new JSONObject();
					bpfValue.put(property.getName().toString(), property.getValue());	
					bpfValues.add(bpfValue);
				}
			}		
			bpf.put("data", bpfValues);
			resultsArray.add(bpf);
		}
		iterator.close();

		result.put("result", resultsArray);
		LOGGER.debug(result.toString());
		return result.toString();
	}

}
