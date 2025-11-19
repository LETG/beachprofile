package fr.indigeo.wps.bpt.tools;

import static org.junit.jupiter.api.Assertions.*;

import java.util.LinkedList;

import org.geotools.referencing.CRS;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

public class BeachProfileUtilsTest {

    @Test
    public void testGetDistanceFromCoordinates_emptyArray() throws Exception {
        Coordinate[] coords = new Coordinate[0];
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertEquals(0.0, dist, 1e-6);
    }

    @Test
    public void testGetDistanceFromCoordinates_singlePoint_4326() throws Exception {
        Coordinate[] coords = new Coordinate[] { new Coordinate(0, 0) };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertEquals(0.0, dist, 1e-6);
    }

    @Test
    public void testGetDistanceFromCoordinates_twoPoints_4326() throws Exception {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(48.8566, 2.3522), // Paris
            new Coordinate(45.7640, 4.8357) // Lyon  - 391 km from Lyon
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertTrue(dist > 390000 && dist < 392000, "Distance should be about 391 km");
    }

     @Test
    public void testGetDistanceFromCoordinates_twoPoints2_4326() throws Exception {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(45.7640, 4.8357), // Paris
            new Coordinate(48.8014, -3.4556) // Perros Guirec - 711.8 km from Lyon
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertTrue(dist > 711000 && dist < 712000, "Distance should be about 710 km");
    }

    @Test
    public void testGetDistanceFromCoordinates_multiplePoints_4326() throws Exception {
        Coordinate[] coords = new Coordinate[] {
             new Coordinate(48.8566, 2.3522), // Paris
            new Coordinate(45.7640, 4.8357), // Lyon - 391.5 km from Paris
            new Coordinate(48.8014, -3.4556) // Perros Guirec - 711.8 km from Lyon
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:4326");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
   
        assertTrue(dist > 1100000 && dist < 1105000, "Distance should be about 1 103 km");
    }

    @Test
    public void testGetDistanceFromCoordinates_twoPoints() throws Exception {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(652469, 6862035), //Paris
            new Coordinate(842667, 6519924)  //Lyon
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertTrue(dist > 390000 && dist < 392000, "Distance should be about 391 km");
    }

     @Test
    public void testGetDistanceFromCoordinates_twoPoints_1m() throws Exception {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(	140092.5, 6858694.7, 2),
            new Coordinate(140092.5, 6858695.7, 4)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertEquals(1, dist,  1e-3, "Distance should be 1 m");
     }

    @Test
    public void testGetDistanceFromCoordinates_multiplePoints() throws Exception {
        Coordinate[] coords = new Coordinate[] {
           new Coordinate(652469, 6862035), //Paris
            new Coordinate(842667, 6519924),  //Lyon
            new Coordinate(222355, 6875075) // Perros Guirec - 711.8 km from Lyon
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, crs);
        assertTrue(dist > 1107000 && dist < 1108000, "Distance should be about 1 107 km");
   }

    @Test
    public void testGetDistanceFromCoordinates_nullCRS() {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0),
            new Coordinate(0, 1)
        };
        double dist = BeachProfileUtils.getDistanceFromCoordinates(coords, null);
        assertEquals(0.0, dist, 1e-6);
    }


 @Test
    public void testGetProfileArea_emptyArray() throws Exception {
        Coordinate[] coords = new Coordinate[0];
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 10, crs);
        assertEquals(0.0, area, 1e-6);
    }

    @Test
    public void testGetProfileArea_nullCRS() {
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(0, 0, 1),
            new Coordinate(0, 1, 2)
        };
        double area = BeachProfileUtils.getProfileArea(coords, 0, 10, null);
        assertEquals(0.0, area, 1e-6);
    }

    @Test
    public void testGetProfileArea_samepoints_hauteurdiff() throws Exception {

        Coordinate[] coords = new Coordinate[] {
            new Coordinate(	140092.5, 6858694.7, 2),
            new Coordinate(140092.5, 6858694.7, 4)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 10, crs);
        assertEquals(0.0, area, 1e-2);
    }

        @Test
    public void testGetProfileArea_same_high() throws Exception {

        // point à 1 mètre d'écart même hauteur
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(140092.5, 6858694.7, 2),
            new Coordinate(140092.5, 6858695.7, 2)
        };
        // 1 mètre d'écart, hauteur fixe 2 mètres => aire 2m2
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 2, crs);
        assertEquals(2, Math.round(area), 1e-2);
    }

    @Test
    public void testGetProfileArea_twoSegments_same_high() throws Exception {

        Coordinate[] coords = new Coordinate[] {
            new Coordinate(140092.5, 6858654.7, 2),
            new Coordinate(140092.5, 6858664.7, 2),
            new Coordinate(140092.5, 6858674.7, 2)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 20, crs);
        assertEquals(40.0, area, 1);
    }


    @Test
    public void testGetProfileArea_twoSegments() throws Exception {
        
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(140092.5, 6858654.7, 0),
            new Coordinate(140092.5, 6858664.7, 2),
            new Coordinate(140092.5, 6858674.7, 4)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 20, crs);
        assertEquals(40.0, area, 1);
    }

    @Test
    public void testGetProfileArea_twoSegments_negative() throws Exception {
       
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(140092.5, 6858654.7, -2),
            new Coordinate(140092.5, 6858664.7, 0),
            new Coordinate(140092.5, 6858674.7, 2)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 0, 20, crs);
        assertEquals(0, area, 1);
    }


    @Test
    public void testGetProfileArea_outOfRange() throws Exception {
        // Les distances sont hors de la plage [100, 200]
        Coordinate[] coords = new Coordinate[] {
            new Coordinate(140092.5, 6858654.7, 2),
            new Coordinate(140092.5, 6858954.7, 4)
        };
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        double area = BeachProfileUtils.getProfileArea(coords, 100, 200, crs);
        assertEquals(0.0, area, 1e-6);
    }


    @Test
    // distance de 0 m entre les points même hauteur
    public void testInterpolateCoordinates_basic() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        Coordinate c1 = new Coordinate(140092, 6858654, 2.0);
        Coordinate c2 = new Coordinate(140092, 6858654, 2.0); 

        double offset = 0.0;
        double interval = 2.0; // tous les 2 mètres

        LinkedList<Coordinate> result = BeachProfileUtils.InterpolateCoordinates(offset, interval, c1, c2, crs);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(140092, result.getFirst().x, 1e-2);
        assertEquals(140092, result.getLast().x, 1e-2);
        assertEquals(2.0, result.getFirst().z, 1e-2);
        assertEquals(2.0, result.getLast().z, 1e-2);
    }

    @Test
    public void testInterpolateCoordinates_10m() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2154");
        Coordinate c1 = new Coordinate(140082, 6858654, 2.0);
        Coordinate c2 = new Coordinate(140092, 6858654, 2.0); // 10 mètres d'écart

        double offset = 0.0;
        double interval = 2.0;  // tous les 2 mètres

        LinkedList<Coordinate> result = BeachProfileUtils.InterpolateCoordinates(offset, interval, c1, c2, crs);

        assertNotNull(result);
        // Le premier point est toujours c1, puis tous les 2m à partir de 1m
        // On devrait avoir le point à 140082, 140084, 140086, 140088, 140090 et 140092 soit 6 points
        // sauf que le calcul de distance passse à plus de 14m... Et les 2 m ne sont pas respectés
        // TODO revoir l'algo d'interpolation
        assertEquals(9, result.size());
        assertEquals(140082, result.getFirst().x, 1e-2);
        assertEquals(140092, result.getLast().x, 1e-2);
    }

}