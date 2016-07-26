<%--
  ao-web-page-taglib - Java API for modeling web page content and relationships in a JSP environment.
  Copyright (C) 2013, 2014, 2016  AO Industries, Inc.
    support@aoindustries.com
    7262 Bull Pen Cir
    Mobile, AL 36695

  This file is part of ao-web-page-taglib.

  ao-web-page-taglib is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  ao-web-page-taglib is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with ao-web-page-taglib.  If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page language="java" buffer="512kb" autoFlush="true" pageEncoding="UTF-8" session="false" %>
<%@include file="taglibs.inc.jsp" %>
<c:if test="${!requestScope['/ao-web-page-taglib/head.inc.jsp/included']}">
	<c:set scope="request" var="/ao-web-page-taglib/head.inc.jsp/included" value="true" />
	<ao:script src="/ao-web-page-servlet/scripts.js" />
	<c:if test="${!header['X-com-aoindustries-web-page-exporting']}">
		<ao:script>
			ao_web_page_servlet.openFileUrl = <ao:out value="${ao:encodeURL(pageContext.request.contextPath.concat('/ao-web-page-taglib/ajax/open-file.jsp'))}" />;
		</ao:script>
	</c:if>
</c:if>