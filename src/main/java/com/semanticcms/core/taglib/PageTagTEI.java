/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.collections.MinimalList;
import com.aoindustries.encoding.Doctype;
import com.aoindustries.encoding.Serialization;
import java.util.List;
import java.util.Locale;
import javax.servlet.jsp.tagext.TagData;
import javax.servlet.jsp.tagext.TagExtraInfo;
import javax.servlet.jsp.tagext.ValidationMessage;

/**
 * @author  AO Industries, Inc.
 */
public class PageTagTEI extends TagExtraInfo {

	@Override
	public ValidationMessage[] validate(TagData data) {
		List<ValidationMessage> messages = MinimalList.emptyList();
		Object serializationAttr = data.getAttribute("serialization");
		if(
			serializationAttr != null
			&& serializationAttr != TagData.REQUEST_TIME_VALUE
		) {
			String serialization = ((String)serializationAttr).trim();
			if(!serialization.isEmpty() && !"auto".equalsIgnoreCase(serialization)) {
				try {
					Serialization.valueOf(serialization.toUpperCase(Locale.ROOT));
				} catch(IllegalArgumentException e) {
					messages = MinimalList.add(
						messages,
						new ValidationMessage(data.getId(), com.aoindustries.taglib.Resources.RESOURCES.getMessage("HtmlTag.serialization.invalid", serialization))
					);
				}
			}
		}
		Object doctypeAttr = data.getAttribute("doctype");
		if(
			doctypeAttr != null
			&& doctypeAttr != TagData.REQUEST_TIME_VALUE
		) {
			String doctype = ((String)doctypeAttr).trim();
			if(!doctype.isEmpty() && !"default".equalsIgnoreCase(doctype)) {
				try {
					Doctype.valueOf(doctype.toUpperCase(Locale.ROOT));
				} catch(IllegalArgumentException e) {
					messages = MinimalList.add(
						messages,
						new ValidationMessage(data.getId(), com.aoindustries.taglib.Resources.RESOURCES.getMessage("HtmlTag.doctype.invalid", doctype))
					);
				}
			}
		}
		return messages.isEmpty() ? null : messages.toArray(new ValidationMessage[messages.size()]);
	}
}
