package com.pinterest.teletraan.exception;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Scanner;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

// Thanks to http://stackoverflow.com/questions/19621653/how-should-i-log-uncaught-exceptions-in-my-restful-jax-rs-web-service
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericExceptionMapper.class);

    @Context
    HttpServletRequest request;

    @Override
    public Response toResponse(Throwable t) {
        if (t instanceof WebApplicationException) {
            return ((WebApplicationException) t).getResponse();
        } else {
            String errorMessage = buildErrorMessage(request);
            LOG.error(errorMessage, t);

            StringBuilder sb = new StringBuilder(errorMessage);
            if (t.getMessage() != null) {
                sb.append("\n");
                sb.append(t.getMessage());
            }
            StringWriter sw = new StringWriter();
            PrintWriter pw = new PrintWriter(sw);
            t.printStackTrace(pw);
            sb.append(sw.toString());
            return Response.serverError().entity(sb.toString()).build();
        }
    }

    private String buildErrorMessage(HttpServletRequest req) {
        StringBuilder message = new StringBuilder();
        String entity = "";

        try {
            InputStream is = req.getInputStream();
            Scanner s = new Scanner(is, "UTF-8").useDelimiter("\\A");
            entity = s.hasNext() ? s.next() : entity;
        } catch (Exception ex) {
            // Ignore exceptions around getting the entity
        }

        message.append("Teletraan Exception:\n");
        message.append("API: ").append(getOriginalURL(req)).append("\n");
        message.append("Method: ").append(req.getMethod()).append("\n");
        message.append("Entity: ").append(entity);
        return message.toString();
    }

    private String getOriginalURL(HttpServletRequest req) {
        String scheme = req.getScheme();
        String serverName = req.getServerName();
        int serverPort = req.getServerPort();
        String contextPath = req.getContextPath();
        String servletPath = req.getServletPath();
        String pathInfo = req.getPathInfo();
        String queryString = req.getQueryString();

        StringBuilder url = new StringBuilder();
        url.append(scheme).append("://").append(serverName);

        if (serverPort != 80 && serverPort != 443) {
            url.append(":").append(serverPort);
        }

        url.append(contextPath).append(servletPath);

        if (pathInfo != null) {
            url.append(pathInfo);
        }

        if (queryString != null) {
            url.append("?").append(queryString);
        }

        return url.toString();
    }
}
