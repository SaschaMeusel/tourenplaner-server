/*
 * Copyright 2012 ToureNPlaner
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */

package de.tourenplaner.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * This utility class is used to convert the result of a /algsp request to gpx
 * and prints it on stdout
 *
 * @author Christoph Haag, Sascha Meusel, Niklas Schnelle, Peter Vollmer
 */
public class SPtoGPX {
    @SuppressWarnings("unchecked")
	public static void main(String[] args) {
		if (args.length != 1) {
			System.err
					.println("Please supply a .json file as first parameter or - to read from stdin");
			return;
		}
		InputStream input;
		try {
			input = args[0].equals("-") ? System.in : new FileInputStream(
					args[0]);
		} catch (FileNotFoundException e) {
			System.err.println("The file: " + args[0] + " could not be found");
			return;
		}
		ObjectMapper mapper = new ObjectMapper();

		try {
			Map<String, Object> requestJSON = mapper.readValue(input,
					new TypeReference<Map<String, Object>>() {
					});
			@SuppressWarnings("unchecked")
            List<Object> subways = (List<Object>) requestJSON.get("way");
			List<Map<String, Object>> points;

			System.out
					.println("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\" ?>");
			System.out
					.println("<gpx xmlns=\"http://www.topografix.com/GPX/1/1\" xmlns:gpxx=\"http://www.garmin.com/xmlschemas/GpxExtensions/v3\" xmlns:gpxtpx=\"http://www.garmin.com/xmlschemas/TrackPointExtension/v1\" creator=\"Oregon 400t\" version=\"1.1\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.topografix.com/GPX/1/1 http://www.topografix.com/GPX/1/1/gpx.xsd http://www.garmin.com/xmlschemas/GpxExtensions/v3 http://www.garmin.com/xmlschemas/GpxExtensionsv3.xsd http://www.garmin.com/xmlschemas/TrackPointExtension/v1 http://www.garmin.com/xmlschemas/TrackPointExtensionv1.xsd\">");
			System.out.println("  <trk>\n"
					+ "    <name>Example GPX Document</name>");

			for (Object subway : subways) {
                System.out.println("<trkseg>");

                points = (List<Map<String, Object>>) subway;
                for (Map<String, Object> point : points) {
                    System.out.println("<trkpt lat=\""
                            + ((Integer) point.get("lt")).doubleValue()
                            / 10000000.0 + "\" lon=\""
                            + ((Integer) point.get("ln")).doubleValue()
                            / 10000000.0 + "\"></trkpt>");
                }
                System.out.println("</trkseg>\n");
            }
			System.out.println("</trk>\n</gpx>");

		} catch (IOException e) {
			System.err.println("An IO Error ocurred: " + e.getMessage());
		}
	}
}
