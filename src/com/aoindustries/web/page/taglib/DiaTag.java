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

import static com.aoindustries.encoding.TextInXhtmlAttributeEncoder.encodeTextInXhtmlAttribute;
import com.aoindustries.net.UrlUtils;
import com.aoindustries.servlet.http.LastModifiedServlet;
import com.aoindustries.web.page.DiaExport;
import com.aoindustries.web.page.PageRef;
import com.aoindustries.web.page.servlet.CaptureLevel;
import com.aoindustries.web.page.servlet.DiaExportServlet;
import com.aoindustries.web.page.servlet.LinkImpl;
import com.aoindustries.web.page.servlet.PageRefResolver;
import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

public class DiaTag extends SimpleTagSupport {

	private static final String MISSING_IMAGE_PATH = "/lib/images/missing-image.jpg";
	private static final int MISSING_IMAGE_WIDTH = 225;
	private static final int MISSING_IMAGE_HEIGHT = 224;

	private String book;
	public void setBook(String book) {
		this.book = book;
	}

	private String path;
	public void setPath(String path) {
		this.path = path;
    }

	private int width;
	public void setWidth(int width) {
		this.width = width;
	}
	
	private int height;
	public void setHeight(int height) {
		this.height = height;
	}

	/**
	 * Writes an img tag for the diagram thumbnail.
	 */
	@Override
    public void doTag() throws IOException, JspTagException {
		final PageContext pageContext = (PageContext)getJspContext();
		final HttpServletRequest request = (HttpServletRequest)pageContext.getRequest();

		// Get the current capture state
		final CaptureLevel captureLevel = CaptureLevel.getCaptureLevel(request);
		if(captureLevel.compareTo(CaptureLevel.META) >= 0) {
			final ServletContext servletContext = pageContext.getServletContext();
			PageRef pageRef;
			try {
				pageRef = PageRefResolver.getPageRef(servletContext, request, this.book, this.path);
			} catch(ServletException e) {
				throw new JspTagException(e);
			}
			if(captureLevel == CaptureLevel.BODY) {
				final HttpServletResponse response = (HttpServletResponse)pageContext.getResponse();
				final String responseEncoding = response.getCharacterEncoding();
				final JspWriter out = pageContext.getOut();
				// Use default width when neither provided
				if(width==0 && height==0) width = DiaExportServlet.DEFAULT_WIDTH;
				File resourceFile = pageRef.getResourceFile(false, true);
				// Get the thumbnail image
				DiaExport thumbnail =
					resourceFile==null
					? null
					: DiaExport.exportDiagram(
						pageRef,
						width==0 ? null : (width * DiaExportServlet.OVERSAMPLING),
						height==0 ? null : (height * DiaExportServlet.OVERSAMPLING),
						(File)servletContext.getAttribute("javax.servlet.context.tempdir" /*ServletContext.TEMPDIR*/)
					)
				;
				// Write the img tag
				out.write("<img src=\"");
				final String urlPath;
				if(thumbnail != null) {
					StringBuilder urlPathSB = new StringBuilder();
					urlPathSB
						.append(request.getContextPath())
						.append("/lib/docs/dia-export?book=")
						.append(URLEncoder.encode(pageRef.getBookName(), responseEncoding))
						.append("&path=")
						.append(URLEncoder.encode(pageRef.getPath(), responseEncoding));
					if(width != 0) {
						urlPathSB
							.append("&width=")
							.append(width * DiaExportServlet.OVERSAMPLING)
						;
					}
					if(height != 0) {
						urlPathSB
							.append("&height=")
							.append(height * DiaExportServlet.OVERSAMPLING)
						;
					}
					// Check for header disabling auto last modified
					if(!"false".equalsIgnoreCase(request.getHeader(LastModifiedServlet.LAST_MODIFIED_HEADER_NAME))) {
						urlPathSB
							.append('&')
							.append(LastModifiedServlet.LAST_MODIFIED_PARAMETER_NAME)
							.append('=')
							.append(LastModifiedServlet.encodeLastModified(thumbnail.getTmpFile().lastModified()))
						;
					}
					urlPath = urlPathSB.toString();
				} else {
					urlPath =
						request.getContextPath()
						+ MISSING_IMAGE_PATH
					;
				}
				encodeTextInXhtmlAttribute(
					response.encodeURL(
						UrlUtils.encodeUrlPath(
							urlPath,
							responseEncoding
						)
					),
					out
				);
				out.write("\" width=\"");
				encodeTextInXhtmlAttribute(
					Integer.toString(
						thumbnail!=null
						? (thumbnail.getWidth() / DiaExportServlet.OVERSAMPLING)
						: width!=0
						? width
						: (MISSING_IMAGE_WIDTH * height / MISSING_IMAGE_HEIGHT)
					),
					out
				);
				out.write("\" height=\"");
				encodeTextInXhtmlAttribute(
					Integer.toString(
						thumbnail!=null
						? (thumbnail.getHeight() / DiaExportServlet.OVERSAMPLING)
						: height!=0
						? height
						: (MISSING_IMAGE_HEIGHT * width / MISSING_IMAGE_WIDTH)
					),
					out
				);
				out.write("\" alt=\"");
				if(resourceFile == null) {
					LinkImpl.writeBrokenPathInXhtmlAttribute(pageRef, out);
				} else {
					encodeTextInXhtmlAttribute(resourceFile.getName(), out);
				}
				out.write("\" />");
			}
		}
	}
}
