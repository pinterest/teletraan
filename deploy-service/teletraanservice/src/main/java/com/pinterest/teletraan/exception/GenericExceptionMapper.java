package com.pinterest.teletraan.exception;


import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;
import javax.ws.rs.ext.Provider;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.pinterest.deployservice.common.Constants;

// Thanks to http://stackoverflow.com/questions/19621653/how-should-i-log-uncaught-exceptions-in-my-restful-jax-rs-web-service
@Provider
public class GenericExceptionMapper implements ExceptionMapper<Throwable> {
    private static final Logger LOG = LoggerFactory.getLogger(GenericExceptionMapper.class);

    @Context
    HttpServletRequest request;

    private String clientError;

    public GenericExceptionMapper(String clientError) {
        this.clientError = clientError;
    }

    @Override
    public Response toResponse(Throwable t) {
        LOG.error("Server error:", t);
        StringBuilder sb = new StringBuilder();
        if (t.getMessage() != null) {
            sb.append("Message: ").append(t.getMessage());
        }
        if (!Constants.CLIENT_ERROR_SHORT.equals(clientError)) {
            sb.append(buildErrorMessage(request, t));
        }

        if (t instanceof WebApplicationException) {
            return Response.status(((WebApplicationException) t).getResponse().getStatus()).entity(sb.toString())
                    .build();
        } else {
            return Response.serverError().entity(sb.toString()).build();
        }
    }

    private String buildErrorMessage(HttpServletRequest req, Throwable t) {
        StringBuilder message = new StringBuilder();
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        t.printStackTrace(pw);

        message.append("\nResource: ").append(getOriginalURL(req));
        message.append("\nMethod: ").append(req.getMethod());
        message.append("\nStack:\n").append(sw.toString());
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
