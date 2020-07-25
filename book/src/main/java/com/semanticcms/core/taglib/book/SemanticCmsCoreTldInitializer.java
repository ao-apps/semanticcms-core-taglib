/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2016, 2017, 2019, 2020  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of semanticcms-core-taglib.
 *
 * semanticcms-core-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * semanticcms-core-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with semanticcms-core-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.semanticcms.core.taglib.book;

import com.semanticcms.tagreference.TagReferenceInitializer;

public class SemanticCmsCoreTldInitializer extends TagReferenceInitializer {

	public SemanticCmsCoreTldInitializer() {
		super(
			Maven.properties.getProperty("documented.name") + " Reference",
			"Taglib Reference",
			"/core/taglib",
			"/semanticcms-core.tld",
			true,
			Maven.properties.getProperty("documented.javadoc.link.javase"),
			Maven.properties.getProperty("documented.javadoc.link.javaee"),
			// Self
			"com.semanticcms.core.taglib", Maven.properties.getProperty("project.url") + "apidocs/",
			// Dependencies
			"com.aoindustries.net", "https://aoindustries.com/ao-net-types/apidocs/",
			"com.aoindustries.taglib", "https://aoindustries.com/ao-taglib/apidocs/",
			"com.semanticcms.core.model", "https://semanticcms.com/core/model/apidocs/",
			"com.semanticcms.core.servlet", "https://semanticcms.com/core/servlet/apidocs/",
			"com.semanticcms.core.servlet.impl", "https://semanticcms.com/core/servlet/apidocs/"
		);
	}
}
