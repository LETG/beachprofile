package tests;

import java.io.File;
import java.io.IOException;

import tools.BeachProfileTracking;
import tools.FeatureCollectionValidation;
import tools.GeoJsonUtils;

/**
 * Class used for local testing of methods instead of build a wps 
 * @author Quentin Lechat
 *
 */
public class TestGeoJson {

	public static void main(String[] args) {
		long startTime = System.nanoTime();

		File dataDir = new File("data");
		File beachProfileFile = new File(dataDir, "profil_test3_lambert.json");
		
		BeachProfileTracking bp = new BeachProfileTracking();
		
		FeatureCollectionValidation fcv = new FeatureCollectionValidation();
		try {
			System.out.println(bp.featureToCSV(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.1, true, 0, 10)));
			bp.createCSVFile(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.1, true, 0, 10), dataDir, "result.csv");
		} catch (IOException e) {
			e.printStackTrace();
		}

		//get the execution time
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/1000000 ;
		System.out.println(duration + "ms");
		
	}
}
