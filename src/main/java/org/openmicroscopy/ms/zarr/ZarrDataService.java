/*
 * Copyright (C) 2018-2020 University of Dundee & Open Microscopy Environment.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package org.openmicroscopy.ms.zarr;

import java.io.FileInputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Properties;

import io.vertx.core.AsyncResult;
import io.vertx.core.Verticle;
import io.vertx.core.Vertx;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Microservice providing image data over a HTTP endpoint.
 * @author m.t.b.carroll@dundee.ac.uk
 */
public class ZarrDataService {

    /**
     * Obtains the OMERO data source configured from Java system properties (may be loaded from configuration files named in
     * arguments) then starts a verticle that listens on HTTP for queries, checks the database then responds in JSON.
     * @param argv filename(s) from which to read configuration beyond current Java system properties
     * @throws IOException if the configuration could not be loaded
     */
    public static void main(String[] argv) throws IOException {
        mainVerticle(argv, null);
    }

    /**
     * Run the microservice configured using properties files with optional overrides
     * @param argv filename(s) from which to read configuration beyond current Java system properties
     * @param overrides override properties obtained from files with these properties
     * @throws IOException if the configuration could not be loaded
     */
    static void mainVerticle(String[] argv, Properties overrides) throws IOException {
        /* set system properties from named configuration files */
        final Properties propertiesSystem = System.getProperties();
        for (final String filename : argv) {
            final Properties propertiesNew = new Properties();
            try (final InputStream filestream = new FileInputStream(filename)) {
                propertiesNew.load(filestream);
            }
            propertiesSystem.putAll(propertiesNew);
        }
        if (overrides != null) {
            propertiesSystem.putAll(overrides);
        }
        /* use the application context to start enough of OMERO.server to obtain the verticle */
        final AbstractApplicationContext zarrContext = new ClassPathXmlApplicationContext("zarr-context.xml");
        final ApplicationContext omeroContext = zarrContext.getBean("zarr.data", ApplicationContext.class);
        final Verticle verticle = omeroContext.getBean("zarrVerticle", ZarrDataVerticle.class);
        /* deploy the verticle */
        final Vertx vertx = Vertx.vertx();
        vertx.deployVerticle(verticle, (AsyncResult<String> result) -> {
            zarrContext.close();
        });
    }
}
