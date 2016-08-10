<%--
  semanticcms-core-taglib - Java API for modeling web page content and relationships in a JSP environment.
  Copyright (C) 2013, 2014, 2016  AO Industries, Inc.
    support@aoindustries.com
    7262 Bull Pen Cir
    Mobile, AL 36695

  This file is part of semanticcms-core-taglib.

  semanticcms-core-taglib is free software: you can redistribute it and/or modify
  it under the terms of the GNU Lesser General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  semanticcms-core-taglib is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU Lesser General Public License for more details.

  You should have received a copy of the GNU Lesser General Public License
  along with semanticcms-core-taglib.  If not, see <http://www.gnu.org/licenses/>.
--%>
<%@ page language="java" buffer="512kb" autoFlush="true" pageEncoding="UTF-8" session="false" %>
<%@include file="taglibs.inc.jsp" %>
<c:if test="${!requestScope['/semanticcms-core-taglib/head.inc.jsp/included']}">
	<c:set scope="request" var="/semanticcms-core-taglib/head.inc.jsp/included" value="true" />
	<ao:script src="/semanticcms-core-servlet/scripts.js" />
	<c:if test="${!header['X-com-aoindustries-web-page-exporting']}">
		<ao:script>
			semanticcms_core_servlet.openFileUrl = <ao:out value="${ao:encodeURL(pageContext.request.contextPath.concat('/semanticcms-core-servlet/ajax/open-file'))}" />;
		</ao:script>
	</c:if>
</c:if>