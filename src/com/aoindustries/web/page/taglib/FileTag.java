/*
 * ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016  AO Industries, Inc.
 *     support@aoindustries.com
 *     7262 Bull Pen Cir
 *     Mobile, AL 36695
 *
 * This file is part of ao-web-page-taglib.
 *
 * ao-web-page-taglib is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ao-web-page-taglib is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ao-web-page-taglib.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.aoindustries.web.page.taglib;

import static com.aoindustries.encoding.JavaScriptInXhtmlAttributeEncoder.encodeJavaScriptInXhtmlAttribute;
import com.aoindustries.encoding.NewEncodingUtils;
import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import static com.aoindustries.encoding.TextInXhtmlEncoder.encodeTextInXhtml;
import com.aoindustries.io.NullWriter;
import com.aoindustries.net.UrlUtils;
import com.aoindustries.servlet.http.LastModifiedServlet;
import com.aoindustries.util.StringUtility;
import com.aoindustries.web.page.Node;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.CurrentNode;
import com.aoindustries.web.page.servlet.Headers;
import com.aoindustries.web.page.servlet.LinkImpl;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.File;
import java.io.IOException;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.JspFragment;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class FileTag extends SimpleTagSupport {

	private String book;
	public void setBook(String book) {
		this.book = book;
	}

	private String path;
	public void setPath(String path) {
		this.path = path;
    }

	private boolean hidden;
	public void setHidden(boolean hidden) {
		this.hidden = hidden;
	}

	@Override
    public void doTag() throws JspException, IOException {
		final PageContext pageContext = (PageContext)getJspContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		// Get the current capture state
		final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			final ServletContext servletContext = pageContext.getServletContext();
			PageRef file;
			try {
				file = PageRefResolver.getPageRef(servletContext, request, this.book, this.path);
			} catch(ServletException e) {
				throw new JspTagException(e);
			}
			// If we have a parent node, associate this file with the node
			final Node currentNode = CurrentNode.getCurrentNode(request);
			if(currentNode != null && !hidden) currentNode.addFile(file);

			if(captureLevel == CaptureLevel.BODY) {
				// Write a link to the file
				writeFileLink(
					servletContext,
					request,
					(HttpServletResponse)pageContext.getResponse(),
					pageContext.getOut(),
					getJspBody(),
					file
				);
			} else {
				// Invoke body for any meta data, but discard any output
				JspFragment body = getJspBody();
				if(body != null) body.invoke(NullWriter.getInstance());
			}
		}
	}

	static void writeFileLink(
		ServletContext servletContext,
		HttpServletRequest request,
		HttpServletResponse response,
		JspWriter out,
		JspFragment body,
		PageRef file
	) throws JspException, IOException {
		// Determine if local file opening is allowed
		final boolean isAllowed = OpenFileTag.isAllowed(servletContext, request);
		final boolean isExporting = Headers.EXPORTING_HEADER_VALUE.equalsIgnoreCase(request.getHeader(Headers.EXPORTING_HEADER));

		// Find the local file, assuming relative to CVSWORK directory
		File resourceFile = file.getResourceFile(false, true);
		boolean isDirectory;
		if(resourceFile == null) {
			// In other book and not available, assume directory when ends in path separator
			isDirectory = file.getPath().endsWith("/");
		} else {
			// In accessible book, use attributes
			isDirectory = resourceFile.isDirectory();
		}
		out.write("<a");
		if(body == null) {
			out.write(" class=\"");
			out.write(isDirectory ? "directoryLink" : "fileLink");
			out.write('"');
		}
		out.write(" href=\"");
		if(
			isAllowed
			&& resourceFile != null
			&& !isExporting
		) {
			encodeTextInXhtmlAttribute(resourceFile.toURI().toString(), out);
		} else {
			final String urlPath;
			if(
				resourceFile != null
				&& !isDirectory
				// Check for header disabling auto last modified
				&& !"false".equalsIgnoreCase(request.getHeader(LastModifiedServlet.LAST_MODIFIED_HEADER_NAME))
			) {
				// Include last modified on file
				urlPath = request.getContextPath()
					+ file.getBookPrefix()
					+ file.getPath()
					+ "?" + LastModifiedServlet.LAST_MODIFIED_PARAMETER_NAME
					+ "=" + LastModifiedServlet.encodeLastModified(resourceFile.lastModified())
				;
			} else {
				urlPath = request.getContextPath()
					+ file.getBookPrefix()
					+ file.getPath()
				;
			}
			encodeTextInXhtmlAttribute(
				response.encodeURL(
					UrlUtils.encodeUrlPath(
						urlPath,
						response.getCharacterEncoding()
					)
				),
				out
			);
		}
		out.write('"');
		if(
			isAllowed
			&& resourceFile != null
			&& !isExporting
		) {
			out.write(" onclick=\"");
			encodeJavaScriptInXhtmlAttribute("docs.openFile(\"", out);
			NewEncodingUtils.encodeTextInJavaScriptInXhtmlAttribute(file.getBook().getName(), out);
			encodeJavaScriptInXhtmlAttribute("\", \"", out);
			NewEncodingUtils.encodeTextInJavaScriptInXhtmlAttribute(file.getPath(), out);
			encodeJavaScriptInXhtmlAttribute("\"); return false;", out);
			out.write('"');
		}
		out.write('>');
		if(body == null) {
			if(resourceFile == null) {
				LinkImpl.writeBrokenPathInXhtml(file, out);
			} else {
				encodeTextInXhtml(resourceFile.getName(), out);
				if(isDirectory) encodeTextInXhtml('/', out);
			}
		} else {
			body.invoke(null);
		}
		out.write("</a>");
		if(body == null && resourceFile != null && !isDirectory) {
			out.write(" (");
			encodeTextInXhtml(StringUtility.getApproximateSize(resourceFile.length()), out);
			out.write(')');
		}
	}
}
