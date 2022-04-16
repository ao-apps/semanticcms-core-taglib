/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2019, 2020, 2021, 2022  AO Industries, Inc.
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
 * along with semanticcms-core-taglib.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.semanticcms.core.taglib;

import com.aoapps.encoding.Doctype;
import com.aoapps.encoding.Serialization;
import com.aoapps.taglib.HtmlTag;
import java.util.ArrayList;
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
		List<ValidationMessage> messages = new ArrayList<>();
		Object serializationAttr = data.getAttribute("serialization");
		if(
			serializationAttr != null
			&& serializationAttr != TagData.REQUEST_TIME_VALUE
		) {
			String serialization = ((String)serializationAttr).trim(); // TODO: normalizeSerialization
			if(!serialization.isEmpty() && !"auto".equalsIgnoreCase(serialization)) {
				try {
					Serialization.valueOf(serialization.toUpperCase(Locale.ROOT));
				} catch(IllegalArgumentException e) {
					messages.add(
						new ValidationMessage(
							data.getId(),
							HtmlTag.RESOURCES.getMessage("serialization.invalid", serialization)
						)
					);
				}
			}
		}
		Object doctypeAttr = data.getAttribute("doctype");
		if(
			doctypeAttr != null
			&& doctypeAttr != TagData.REQUEST_TIME_VALUE
		) {
			String doctype = ((String)doctypeAttr).trim(); // TODO: normalizeDoctype
			if(!doctype.isEmpty() && !"default".equalsIgnoreCase(doctype)) {
				try {
					Doctype.valueOf(doctype.toUpperCase(Locale.ROOT));
				} catch(IllegalArgumentException e) {
					messages.add(
						new ValidationMessage(
							data.getId(),
							HtmlTag.RESOURCES.getMessage("doctype.invalid", doctype)
						)
					);
				}
			}
		}
		Object autonliAttr = data.getAttribute("autonli");
		if(
			autonliAttr != null
			&& autonliAttr != TagData.REQUEST_TIME_VALUE
		) {
			String autonli = ((String)autonliAttr).trim(); // TODO: normalizeAutonli
			if(
				!autonli.isEmpty()
				&& !"auto".equalsIgnoreCase(autonli)
				&& !"true".equalsIgnoreCase(autonli)
				&& !"false".equalsIgnoreCase(autonli)
			) {
				messages.add(
					new ValidationMessage(
						data.getId(),
						HtmlTag.RESOURCES.getMessage("autonli.invalid", autonli)
					)
				);
			}
		}
		Object indentAttr = data.getAttribute("indent");
		if(
			indentAttr != null
			&& indentAttr != TagData.REQUEST_TIME_VALUE
		) {
			String indent = ((String)indentAttr).trim(); // TODO: normalizeIndent
			if(
				!indent.isEmpty()
				&& !"auto".equalsIgnoreCase(indent)
				&& !"true".equalsIgnoreCase(indent)
				&& !"false".equalsIgnoreCase(indent)
			) {
				messages.add(
					new ValidationMessage(
						data.getId(),
						HtmlTag.RESOURCES.getMessage("indent.invalid", indent)
					)
				);
			}
		}
		return messages.isEmpty() ? null : messages.toArray(new ValidationMessage[messages.size()]);
	}
}
