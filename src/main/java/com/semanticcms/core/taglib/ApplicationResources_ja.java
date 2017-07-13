/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2017  AO Industries, Inc.
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
package com.semanticcms.core.taglib;

import com.aoindustries.util.i18n.EditableResourceBundle;
import com.aoindustries.util.i18n.Locales;
import java.io.File;

public final class ApplicationResources_ja extends EditableResourceBundle {

	/**
	 * Do not use directly.
	 */
	public ApplicationResources_ja() {
		super(
			Locales.JAPANESE,
			ApplicationResources.bundleSet,
			new File(System.getProperty("user.home")+"/maven2/ao/semanticcms/core/taglib/src/main/resources/com/semanticcms/core/taglib/ApplicationResources_ja.properties")
		);
	}
}
