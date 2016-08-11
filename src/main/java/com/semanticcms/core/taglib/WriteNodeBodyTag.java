/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2015, 2016  AO Industries, Inc.
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

import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.NodeBodyWriter;
import java.io.IOException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class WriteNodeBodyTag extends SimpleTagSupport {

	private Node node;
	public void setNode(Node node) {
		this.node = node;
	}

	@Override
	public void doTag() throws JspTagException, IOException {
		PageContext pageContext = (PageContext)getJspContext();
		// Buffering made it slower, only about half throughput:
		// BufferedWriter out = new BufferedWriter(pageContext.getOut());
		node.getBody().writeTo(
			new NodeBodyWriter(
				node,
				pageContext.getOut(),
				new PageElementContext(pageContext)
			)
		);
		//out.flush();
	}
}
