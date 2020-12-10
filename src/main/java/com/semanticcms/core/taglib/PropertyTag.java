/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2017, 2020  AO Industries, Inc.
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

import com.aoindustries.encoding.MediaType;
import com.aoindustries.encoding.taglib.EncodingBufferedTag;
import com.aoindustries.io.buffer.BufferResult;
import com.aoindustries.servlet.jsp.LocalizedJspTagException;
import com.aoindustries.taglib.AttributeRequiredException;
import com.aoindustries.taglib.NameAttribute;
import com.aoindustries.taglib.ValueAttribute;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.servlet.CurrentNode;
import java.io.IOException;
import java.io.Writer;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;

public class PropertyTag
	extends EncodingBufferedTag
	implements
		NameAttribute,
		ValueAttribute
{

	private String name;
	private Object value;
	private boolean valueSet = false;
	private Node target;
	private boolean allowExisting = false;

	@Override
	public MediaType getContentType() {
		return MediaType.TEXT;
	}

	@Override
	public MediaType getOutputType() {
		return null;
	}

	@Override
	public void setName(String name) {
		this.name = name;
	}

	@Override
	public void setValue(Object value) {
		this.value = value;
		this.valueSet = true;
	}

	public void setTarget(Node target) {
		this.target = target;
	}

	public void setAllowExisting(boolean allowExisting) {
		this.allowExisting = allowExisting;
	}

	@Override
	protected void doTag(BufferResult capturedBody, Writer out) throws JspException, IOException {
		if(name == null) throw new AttributeRequiredException("name");
		Node resolvedTarget;
		if(target != null) {
			resolvedTarget = target;
		} else {
			resolvedTarget = CurrentNode.getCurrentNode(((PageContext)getJspContext()).getRequest());
			if(resolvedTarget == null) throw new JspTagException("Unable to find parent node for property target");
		}
		boolean propertySet = resolvedTarget.setProperty(
			name,
			valueSet ? value : capturedBody.trim()
		);
		if(!propertySet && !allowExisting) {
			throw new LocalizedJspTagException(
				Resources.PACKAGE_RESOURCES,
				"error.duplicateDynamicElementProperty",
				name
			);
		}
	}
}
