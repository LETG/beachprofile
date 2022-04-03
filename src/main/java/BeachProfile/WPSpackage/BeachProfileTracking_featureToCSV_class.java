package BeachProfile.WPSpackage;

import org.geotools.process.factory.*;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;
import java.awt.image.BufferedImage;

import org.locationtech.jts.geom.*;

import tools.BeachProfileTracking;

public class BeachProfileTracking_featureToCSV_class extends StaticMethodsProcessFactory<BeachProfileTracking_featureToCSV_class> {
	
	protected static BeachProfileTracking callObject;

	public BeachProfileTracking_featureToCSV_class() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_featureToCSV_class.class);
		callObject = new BeachProfileTracking();
	}

	@DescribeProcess(title="BeachProfileTracking_featureToCSV",description="Add a description of BeachProfileTracking_featureToCSV")
	@DescribeResult(name="result",description="A string containing feature collection informations formatted to csv format")
	public static String BeachProfileTracking_featureToCSV(@DescribeParameter(name="featureCollection",description=" the featureCollection we want to display in csv") FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
		String result;
		result = callObject.featureToCSV( featureCollection);

		return result;
	}
}
