package BeachProfile.WPSpackage;

import java.io.IOException;
import java.util.*;

import org.geotools.process.factory.DescribeParameter;
import org.geotools.process.factory.DescribeProcess;
import org.geotools.process.factory.DescribeResult;
import org.geotools.process.factory.StaticMethodsProcessFactory;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;
import java.awt.image.BufferedImage;
import com.vividsolutions.jts.geom.*;

import com.vividsolutions.jts.io.ParseException;

import remoteWpsCall.*;

import tools.FeatureCollectionValidation;
import tools.BeachProfileTracking;

public class BeachProfileTracking_class extends StaticMethodsProcessFactory<BeachProfileTracking_class>
{
		protected static FeatureCollectionValidation callObject_1;
		protected static BeachProfileTracking callObject_2;

	public BeachProfileTracking_class() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_class.class);
		callObject_1 = new FeatureCollectionValidation();
		callObject_2 = new BeachProfileTracking();
	}

	@DescribeProcess(title="BeachProfileTracking",description="Workflow containing the Interpolation, SedimentaryBalance calculation and conversion to csv wps")
	@DescribeResult(name="result",description="A text containing feature collection informations formatted to csv format")
	public static String BeachProfileTracking(@DescribeParameter(name="fc",description=" featureCollection containing the profile geometries") FeatureCollection<SimpleFeatureType, SimpleFeature> fc,@DescribeParameter(name="interval",description=" interval used for the interpolation") Double interval,@DescribeParameter(name="useSmallestDistance",description=" If useSmallestDistance is true, use the smallest distance between all features, else ignore the feature shorter than the first one") Boolean useSmallestDistance,@DescribeParameter(name="minDist",description=" specifie the minimum distance of the interval of calculation") Double minDist,@DescribeParameter(name="maxDist",description=" specifie the maximum distance of the interval of calculation") Double maxDist) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> fc_wps1 ;	
		Double interpolationValue_wps1 = 0.0;	
		Boolean useSmallestDistance_wps1 = true;	
		Double minDist_wps1 = 0.0;	
		Double maxDist_wps1 = 0.0;	
		FeatureCollection<SimpleFeatureType, SimpleFeature> result_wps1 ;
		FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection_wps2 ;	
		String result_wps2 = "";
		String result;
		fc_wps1=fc;
		interpolationValue_wps1=interval;
		useSmallestDistance_wps1=useSmallestDistance;
		minDist_wps1=minDist;
		maxDist_wps1=maxDist;


		result_wps1 = callObject_1.calculWithErrorManager(fc_wps1,interpolationValue_wps1,useSmallestDistance_wps1,minDist_wps1,maxDist_wps1);	
		featureCollection_wps2=result_wps1;
		result_wps2 = callObject_2.featureToCSV(featureCollection_wps2);	
		result=result_wps2;	
		return result;	
	}
}
