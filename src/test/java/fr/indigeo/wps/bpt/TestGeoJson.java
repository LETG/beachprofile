package fr.indigeo.wps.bpt;

import java.io.File;
import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Test;

import fr.indigeo.wps.bpt.tools.BeachProfileTrackingTools;
import fr.indigeo.wps.bpt.tools.FeatureCollectionValidation;
import fr.indigeo.wps.bpt.tools.GeoJsonUtils;

/**
 * Class used for local testing of methods instead of build a wps 
 * @author Quentin Lechat
 *
 */
public class TestGeoJson {

	private static final Logger LOGGER = Logger.getLogger(TestGeoJson.class);

	@Test
	public void testGeoJson () {
		long startTime = System.nanoTime();

		File dataDir = new File("data");
		File beachProfileFile = new File(dataDir, "profil_test3_lambert.json");
		
		BeachProfileTrackingTools bp = new BeachProfileTrackingTools();
		
		FeatureCollectionValidation fcv = new FeatureCollectionValidation();
		try {
			LOGGER.info(bp.featureToCSV(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.1, true, 0, 10)));
			bp.createCSVFile(fcv.calculWithErrorManager(GeoJsonUtils.geoJsonToFeatureCollection(beachProfileFile), 0.1, true, 0, 10), dataDir, "result.csv");
		} catch (IOException e) {
			LOGGER.error("Erreur lors de la crétaion du fichier");
		}

		//get the execution time
		long endTime = System.nanoTime();
		long duration = (endTime - startTime)/1000000 ;
		LOGGER.info("Temps de création : " + duration + "ms");
		
	}
}
