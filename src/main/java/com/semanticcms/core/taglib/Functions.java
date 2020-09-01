/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020  AO Industries, Inc.
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

import com.aoindustries.lang.Strings;
import com.aoindustries.net.URIDecoder;
import com.aoindustries.net.URIEncoder;
import static com.aoindustries.servlet.filter.FunctionContext.getRequest;
import static com.aoindustries.servlet.filter.FunctionContext.getResponse;
import static com.aoindustries.servlet.filter.FunctionContext.getServletContext;
import com.aoindustries.taglib.Link;
import com.semanticcms.core.model.Author;
import com.semanticcms.core.model.Book;
import com.semanticcms.core.model.Copyright;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.servlet.AuthorUtils;
import com.semanticcms.core.servlet.CaptureLevel;
import com.semanticcms.core.servlet.CapturePage;
import com.semanticcms.core.servlet.CopyrightUtils;
import com.semanticcms.core.servlet.Headers;
import com.semanticcms.core.servlet.PageDags;
import com.semanticcms.core.servlet.PageIndex;
import com.semanticcms.core.servlet.PageRefResolver;
import com.semanticcms.core.servlet.PageUtils;
import com.semanticcms.core.servlet.SemanticCMS;
import com.semanticcms.core.servlet.View;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.jsp.JspTagException;

final public class Functions {

	public static Page capturePageInBook(String book, String page, String level) throws ServletException, IOException {
		PageRef pageRef = PageRefResolver.getPageRef(
			getServletContext(),
			getRequest(),
			book,
			page
		);
		if(pageRef.getBook()==null) throw new IllegalArgumentException("Book not found: " + pageRef.getBookName());
		return CapturePage.capturePage(
			getServletContext(),
			getRequest(),
			getResponse(),
			pageRef,
			CaptureLevel.valueOf(level.toUpperCase(Locale.ROOT))
		);
	}

	public static Page capturePage(String page, String level) throws ServletException, IOException, JspTagException {
		return capturePageInBook(null, page, level);
	}

	public static Page captureContentRoot(String level) throws ServletException, IOException {
		ServletContext servletContext = getServletContext();
		return CapturePage.capturePage(
			servletContext,
			getRequest(),
			getResponse(),
			SemanticCMS.getInstance(servletContext).getRootBook().getContentRoot(),
			CaptureLevel.valueOf(level.toUpperCase(Locale.ROOT))
		);
	}

	public static PageIndex getPageIndex(PageRef pageRef) throws ServletException, IOException, JspTagException {
		return PageIndex.getPageIndex(
			getServletContext(),
			getRequest(),
			getResponse(),
			pageRef
		);
	}

	public static List<Page> convertPageDagToList(Page rootPage, String level) throws ServletException, IOException {
		return PageDags.convertPageDagToList(
			getServletContext(),
			getRequest(),
			getResponse(),
			rootPage,
			CaptureLevel.valueOf(level.toUpperCase(Locale.ROOT))
		);
	}

	/**
	 * Gets the current capture level or <code>null</code> if not currently
	 * capturing
	 * 
	 * @see  CaptureLevel
	 */
	public static String getCaptureLevel() {
		final HttpServletRequest request = getRequest();
		final CapturePage capture = CapturePage.getCaptureContext(request);
		if(capture == null) return null;
		return CaptureLevel.getCaptureLevel(request).name().toLowerCase(Locale.ROOT);
	}

	public static File getFileInBook(String book, String path, boolean requireFile) throws ServletException, IOException {
		PageRef pageRef = PageRefResolver.getPageRef(
			getServletContext(),
			getRequest(),
			book,
			path
		);
		if(pageRef.getBook()==null) throw new IllegalArgumentException("Book not found: " + pageRef.getBookName());
		return pageRef.getResourceFile(true, requireFile);
	}

	public static File getFile(String path, boolean requireFile) throws ServletException, IOException {
		return getFileInBook(null, path, requireFile);
	}

	public static File getExeFileInBook(String book, String path) throws ServletException, IOException {
		File file = getFileInBook(book, path, false);
		if(
			!file.canExecute()
			&& !file.setExecutable(true)
		) {
			throw new IOException("Unable to set executable flag: " + file.getPath());
		}
		return file;
	}

	public static File getExeFile(String path) throws ServletException, IOException {
		return getExeFileInBook(null, path);
	}

	/**
	 * @deprecated  Please use {@link URIEncoder#encodeURIComponent(java.lang.String)} instead.
	 */
	@Deprecated
	public static String encodeUrlParam(String value) {
		return URIEncoder.encodeURIComponent(value);
	}

	public static String getRefId(String id) throws ServletException {
		return PageIndex.getRefId(
			getServletContext(),
			getRequest(),
			id
		);
	}

	public static String getRefIdInPage(Page page, String id) {
		return PageIndex.getRefIdInPage(
			getRequest(),
			page,
			id
		);
	}

	public static Double ceil(Double a) {
		return a==null ? null : Math.ceil(a);
	}

	public static Double floor(Double a) {
		return a==null ? null : Math.floor(a);
	}

	// TODO: Move to ao-taglib?
	public static Map<String,String> parseQueryString(String queryString) {
		if(queryString==null) return null;
		List<String> pairs = Strings.split(queryString, '&');
		Map<String,String> params = new LinkedHashMap<>(pairs.size() * 4/3 + 1);
		for(String pair : pairs) {
			int equalPos = pair.indexOf('=');
			String name, value;
			if(equalPos==-1) {
				name = URIDecoder.decodeURIComponent(pair);
				value = "";
			} else {
				// TODO: Avoid substring?
				name = URIDecoder.decodeURIComponent(pair.substring(0, equalPos));
				value = URIDecoder.decodeURIComponent(pair.substring(equalPos + 1));
			}
			if(!params.containsKey(name)) params.put(name, value);
		}
		return params;
	}

	public static String repeat(String value, int count) {
		if(value==null) return null;
		int len = value.length();
		if(len==0) return value;
		StringBuilder sb = new StringBuilder(len * count);
		for(int i=0; i<count; i++) sb.append(value);
		return sb.toString();
	}

	public static Book getBook(String pagePath) {
		if(pagePath==null) return null;
		Book book = SemanticCMS.getInstance(getServletContext()).getBook(pagePath);
		if(book==null) throw new IllegalArgumentException("Book not found: " + pagePath);
		return book;
	}

	public static boolean isExporting() {
		return Headers.isExporting(getRequest());
	}

	public static Copyright findCopyright(Page page) throws ServletException, IOException {
		return CopyrightUtils.findCopyright(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static Set<Author> findAuthors(Page page) throws ServletException, IOException {
		return AuthorUtils.findAuthors(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static boolean findAllowRobots(Page page) throws ServletException, IOException {
		return PageUtils.findAllowRobots(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static boolean hasElement(Page page, String elementType, boolean recursive) throws ServletException, IOException, ClassNotFoundException {
		return PageUtils.hasElement(
			getServletContext(),
			getRequest(),
			getResponse(),
			page,
			Class.forName(elementType).asSubclass(Element.class),
			recursive
		);
	}

	public static List<? extends Element> findTopLevelElements(Node node, String elementType) throws ClassNotFoundException {
		return node.findTopLevelElements(
			Class.forName(elementType).asSubclass(Element.class)
		);
	}

	public static List<? extends Element> filterElements(Page page, String elementType) throws ClassNotFoundException {
		return page.filterElements(
			Class.forName(elementType).asSubclass(Element.class)
		);
	}

	public static boolean isViewApplicable(View view, Page page) throws ServletException, IOException {
		return view.isApplicable(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static String getViewLinkCssClass(View view) throws ServletException, IOException {
		return view.getLinkCssClass(
			getServletContext(),
			getRequest(),
			getResponse()
		);
	}

	public static Map<String,List<String>> getViewLinkParams(View view, Page page) throws ServletException, IOException {
		return view.getLinkParams(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static Copyright getViewCopyright(View view, Page page) throws ServletException, IOException {
		return view.getCopyright(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static Set<Author> getViewAuthors(View view, Page page) throws ServletException, IOException {
		return view.getAuthors(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static String getViewTitle(View view, Page page) throws ServletException, IOException {
		return view.getTitle(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static Collection<Link> getViewLinks(View view, Page page) throws ServletException, IOException {
		if(view == null) return Collections.emptyList();
		return view.getLinks(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static boolean getViewAllowRobots(View view, Page page) throws ServletException, IOException {
		return view.getAllowRobots(
			getServletContext(),
			getRequest(),
			getResponse(),
			page
		);
	}

	public static String getLinkCssClass(Element element) {
		return SemanticCMS.getInstance(getServletContext()).getLinkCssClass(element);
	}

	public static Map<String,String> mergeGlobalAndViewScripts(View view) {
		Map<String,String> globalScripts = SemanticCMS.getInstance(getServletContext()).getScripts();
		Map<String,String> viewScripts = view == null ? null : view.getScripts();

		// Shortcut for when no view scripts
		if(viewScripts == null || viewScripts.isEmpty()) return globalScripts;

		Map<String,String> merged = new LinkedHashMap<>((globalScripts.size() + viewScripts.size())*4/3+1);

		// Add all global scripts
		merged.putAll(globalScripts);

		// Merge per-view scripts
		for(Map.Entry<String,String> entry : viewScripts.entrySet()) {
			String name = entry.getKey();
			String src = entry.getValue();
			String existingSrc = merged.get(name);
			if(existingSrc != null) {
				assert merged.containsKey(name);
				if(!src.equals(existingSrc)) {
					throw new IllegalStateException(
						"Script already registered but with a different src:"
						+ " name=" + name
						+ " src=" + src
						+ " existingSrc=" + existingSrc
					);
				}
			} else {
				if(merged.values().contains(src)) {
					throw new IllegalStateException("Non-unique view script src: " + src);
				}
				if(merged.put(name, src) != null) throw new AssertionError();
			}
		}
		return Collections.unmodifiableMap(merged);
	}

	/**
	 * Make no instances.
	 */
	private Functions() {
	}
}