package BeachProfile.WPSpackage;

import org.geotools.process.factory.*;
import org.geotools.text.Text;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.simple.*;
import java.awt.image.BufferedImage;

import com.vividsolutions.jts.geom.*;

import tools.BeachProfileTracking;

public class BeachProfileTracking_InterpolateFeatureCollection_class extends StaticMethodsProcessFactory<BeachProfileTracking_InterpolateFeatureCollection_class> {
	
	protected static BeachProfileTracking callObject;

	public BeachProfileTracking_InterpolateFeatureCollection_class() {
		super(Text.text("beach profile analysis"),"BeachProfile",BeachProfileTracking_InterpolateFeatureCollection_class.class);
		callObject = new BeachProfileTracking();
	}

	@DescribeProcess(title="BeachProfileTracking_InterpolateFeatureCollection",description="Add a description of BeachProfileTracking_InterpolateFeatureCollection")
	@DescribeResult(name="result",description="the feature collection interpolated")
	public static FeatureCollection<SimpleFeatureType, SimpleFeature> BeachProfileTracking_InterpolateFeatureCollection(@DescribeParameter(name="fc",description=" the feature collection containing geometries we want to interpolate") FeatureCollection<SimpleFeatureType, SimpleFeature> fc,@DescribeParameter(name="interval",description=" distance between coordinates of the geometry") Double interval) {
		FeatureCollection<SimpleFeatureType, SimpleFeature> result;
		result = callObject.InterpolateFeatureCollection( fc, interval);

		return result;
	}
}
