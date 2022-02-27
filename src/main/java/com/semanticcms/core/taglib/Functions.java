/*
 * semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
 * Copyright (C) 2013, 2014, 2015, 2016, 2017, 2018, 2019, 2020, 2021, 2022  AO Industries, Inc.
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

import com.aoapps.collections.AoCollections;
import com.aoapps.lang.Strings;
import com.aoapps.lang.validation.ValidationException;
import com.aoapps.net.DomainName;
import com.aoapps.net.Path;
import com.aoapps.net.URIDecoder;
import static com.aoapps.servlet.filter.FunctionContext.getRequest;
import static com.aoapps.servlet.filter.FunctionContext.getResponse;
import static com.aoapps.servlet.filter.FunctionContext.getServletContext;
import com.aoapps.taglib.Link;
import com.semanticcms.core.controller.AuthorUtils;
import com.semanticcms.core.controller.Book;
import com.semanticcms.core.controller.CapturePage;
import com.semanticcms.core.controller.CopyrightUtils;
import com.semanticcms.core.controller.PageDags;
import com.semanticcms.core.controller.PageRefResolver;
import com.semanticcms.core.controller.PageUtils;
import com.semanticcms.core.controller.ResourceRefResolver;
import com.semanticcms.core.controller.SemanticCMS;
import com.semanticcms.core.model.Author;
import com.semanticcms.core.model.BookRef;
import com.semanticcms.core.model.Copyright;
import com.semanticcms.core.model.Element;
import com.semanticcms.core.model.Node;
import com.semanticcms.core.model.Page;
import com.semanticcms.core.model.PageRef;
import com.semanticcms.core.model.ResourceRef;
import com.semanticcms.core.pages.CaptureLevel;
import com.semanticcms.core.pages.local.CaptureContext;
import com.semanticcms.core.pages.local.CurrentCaptureLevel;
import com.semanticcms.core.renderer.html.Headers;
import com.semanticcms.core.renderer.html.HtmlRenderer;
import com.semanticcms.core.renderer.html.PageIndex;
import com.semanticcms.core.renderer.html.View;
import com.semanticcms.core.resources.Resource;
import com.semanticcms.core.resources.ResourceStore;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

public final class Functions {

	/** Make no instances. */
	private Functions() {throw new AssertionError();}

	public static Page capturePageInDomain(String domain, String book, String page, String level) throws ServletException, IOException, ValidationException {
		ServletContext servletContext = getServletContext();
		PageRef pageRef = PageRefResolver.getPageRef(
			servletContext,
			getRequest(),
			DomainName.valueOf(Strings.nullIfEmpty(domain)),
			Path.valueOf(Strings.nullIfEmpty(book)),
			page
		);
		BookRef bookRef = pageRef.getBookRef();
		if(!SemanticCMS.getInstance(servletContext).getBook(bookRef).isAccessible()) {
			throw new IllegalArgumentException("Book is not accessible: " + bookRef);
		}
		return CapturePage.capturePage(
			getServletContext(),
			getRequest(),
			getResponse(),
			pageRef,
			CaptureLevel.valueOf(level.toUpperCase(Locale.ROOT))
		);
	}

	public static Page capturePageInBook(String book, String page, String level) throws ServletException, IOException, ValidationException {
		return capturePageInDomain(null, book, page, level);
	}

	public static Page capturePage(String page, String level) throws ServletException, IOException {
		try {
			return capturePageInDomain(null, null, page, level);
		} catch(ValidationException e) {
			throw new ServletException(e);
		}
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

	public static PageIndex getPageIndex(PageRef pageRef) throws ServletException, IOException {
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
		final CaptureContext capture = CaptureContext.getCaptureContext(request);
		if(capture == null) return null;
		return CurrentCaptureLevel.getCaptureLevel(request).name().toLowerCase(Locale.ROOT);
	}

	public static Resource getResourceInDomain(String domain, String book, String path, boolean require) throws ServletException, IOException, ValidationException {
		ServletContext servletContext = getServletContext();
		ResourceRef resourceRef = ResourceRefResolver.getResourceRef(
			servletContext,
			getRequest(),
			DomainName.valueOf(Strings.nullIfEmpty(domain)),
			Path.valueOf(Strings.nullIfEmpty(book)),
			path
		);
		BookRef bookRef = resourceRef.getBookRef();
		Book bookObj = SemanticCMS.getInstance(servletContext).getBook(bookRef);
		if(!bookObj.isAccessible()) {
			if(require) {
				throw new FileNotFoundException("Book is not accessible: " + resourceRef);
			} else {
				return null;
			}
		}
		ResourceStore resourceStore = bookObj.getResources();
		if(!resourceStore.isAvailable()) {
			if(require) {
				throw new FileNotFoundException("Restore store is not available: " + resourceRef);
			} else {
				return null;
			}
		}
		Resource resource = resourceStore.getResource(resourceRef.getPath());
		if(require && !resource.exists()) {
			throw new FileNotFoundException("Required resource does not exist: " + resourceRef);
		}
		return resource;
	}

	public static Resource getResourceInBook(String book, String path, boolean require) throws ServletException, IOException, ValidationException {
		return getResourceInDomain(null, book, path, require);
	}

	public static Resource getResource(String path, boolean require) throws ServletException, IOException {
		try {
			return getResourceInDomain(null, null, path, require);
		} catch(ValidationException e) {
			throw new ServletException(e);
		}
	}

	// TODO: Move to a new semanticcms-core-renderer-html-taglib
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

	// TODO: There must be a "jdk" or "java" taglib or similar out there that exposes basic java API stuff?
	//       If not, do we write it?
	public static Double ceil(Double a) {
		return a==null ? null : Math.ceil(a);
	}

	// TODO: There must be a "jdk" or "java" taglib or similar out there that exposes basic java API stuff?
	//       If not, do we write it?
	public static Double floor(Double a) {
		return a==null ? null : Math.floor(a);
	}

	// TODO: Move to ao-taglib?
	public static Map<String, String> parseQueryString(String queryString) {
		if(queryString==null) return null;
		List<String> pairs = Strings.split(queryString, '&');
		Map<String, String> params = AoCollections.newLinkedHashMap(pairs.size());
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
		for(int i = 0; i < count; i++) {
			sb.append(value);
		}
		return sb.toString();
	}

	public static Book getPublishedBook(String pagePath) {
		if(pagePath == null) return null;
		Book book = SemanticCMS.getInstance(getServletContext()).getPublishedBook(pagePath);
		if(book == null) throw new IllegalArgumentException("Book not found: " + pagePath);
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

	public static List<?> findTopLevelElements(Node node, String elementType) throws ClassNotFoundException {
		return node.findTopLevelElements(Class.forName(elementType));
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

	public static Map<String, List<String>> getViewLinkParams(View view, Page page) throws ServletException, IOException {
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

	// TODO: Move to a renderer-html-taglib package, along with other stuff here
	// TODO: Maybe taglib directly in renderer-html.
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

	// TODO: Move to renderer-html-taglib
	public static String getLinkCssClass(Element element) {
		return HtmlRenderer.getInstance(getServletContext()).getLinkCssClass(element);
	}

	public static Map<String, String> mergeGlobalAndViewScripts(View view) {
		Map<String, String> globalScripts = HtmlRenderer.getInstance(getServletContext()).getScripts();
		Map<String, String> viewScripts = view == null ? null : view.getScripts();

		// Shortcut for when no view scripts
		if(viewScripts == null || viewScripts.isEmpty()) return globalScripts;

		Map<String, String> merged = AoCollections.newLinkedHashMap(globalScripts.size() + viewScripts.size());

		// Add all global scripts
		merged.putAll(globalScripts);

		// Merge per-view scripts
		for(Map.Entry<String, String> entry : viewScripts.entrySet()) {
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
}