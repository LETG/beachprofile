package fr.indigeo.wps.bpt.tools;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

public class BeachProfileTrackingToolsTest {

    private final GeometryFactory geometryFactory = new GeometryFactory();
    private final BeachProfileTrackingTools tools = new BeachProfileTrackingTools();

    @Test
    void reprojectFeatureCollectionToRefLine_returnsOriginalWhenRefLineNull() {
        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = new DefaultFeatureCollection();

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, null);

        assertSame(fc, result);
    }

    @Test
    void reprojectFeatureCollectionToRefLine_projectsEachVertexToReference() {
        SimpleFeatureType type = buildLineType();
        DefaultFeatureCollection fc = new DefaultFeatureCollection(null, type);
        DefaultFeatureCollection refLine = new DefaultFeatureCollection(null, type);

        LineString profile = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(1, 0),
            new Coordinate(2, 0)
        });
        fc.add(buildFeature(type, "profile-1", profile));

        LineString reference = geometryFactory.createLineString(new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 5)
        });
        refLine.add(buildFeature(type, "refline-1", reference));

        FeatureCollection<SimpleFeatureType, SimpleFeature> result = tools.reprojectFeatureCollectionToRefLine(fc, refLine);
        SimpleFeature reprojected = result.features().next();
        LineString shifted = (LineString) reprojected.getDefaultGeometry();

        Coordinate[] expected = new Coordinate[] {
            new Coordinate(10, 0),
            new Coordinate(10, 0),
            new Coordinate(10, 0)
        };
        assertArrayEquals(expected, shifted.getCoordinates());
    }

    private SimpleFeatureType buildLineType() {
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName("lineType");
        builder.add("geometry", LineString.class);
        return builder.buildFeatureType();
    }

    private SimpleFeature buildFeature(SimpleFeatureType type, String id, LineString geometry) {
        SimpleFeatureBuilder builder = new SimpleFeatureBuilder(type);
        builder.add(geometry);
        return builder.buildFeature(id);
    }
}
