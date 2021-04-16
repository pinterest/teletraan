package com.pinterest.teletraan.exception;


import com.pinterest.deployservice.common.Constants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.PrintWriter;
import java.io.StringWriter;

import javax.servlet.http.HttpServletRequest;
import javax.validation.ConstraintViolationException;
import javax.validation.ConstraintViolation;
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

    private String clientError;

    public GenericExceptionMapper(String clientError) {
        this.clientError = clientError;
    }

    @Override
    public Response toResponse(Throwable t) {
        LOG.error("Server error:", t);
        if (t instanceof WebApplicationException) {
            StringBuilder sb = new StringBuilder();
            if (t.getMessage() != null) {
                sb.append("\nMessage: ").append(t.getMessage());
            }

            if (clientError.equals(Constants.CLIENT_ERROR_SHORT)) {
                return Response.serverError().entity(sb.toString()).build();
            } else {
                StringWriter sw = new StringWriter();
                PrintWriter pw = new PrintWriter(sw);
                t.printStackTrace(pw);
                sb.append("\n").append(sw.toString());
                return Response.serverError().entity(sb.toString()).build();
            }
        } else if (t instanceof ConstraintViolationException) {
            StringBuilder sb = new StringBuilder();
            ConstraintViolationException cve = (ConstraintViolationException)t;
            for (ConstraintViolation cv : cve.getConstraintViolations()) {
                if (cv.getInvalidValue() != null) {
                    sb.append(cv.getPropertyPath().toString() + ":" + cv.getInvalidValue().toString());
                    sb.append(" " + cv.getMessage());
                }
            }
            sb.append("\nParameters in request violate configured constraints.");
            return Response.status(Response.Status.BAD_REQUEST).entity(sb.toString()).build();
        } else {
            String errorMessage = buildErrorMessage(request);
            StringBuilder sb = new StringBuilder();
            if (t.getMessage() != null) {
                sb.append("\nMessage: ").append(t.getMessage());
            }

            if (clientError.equals(Constants.CLIENT_ERROR_SHORT)) {
                return Response.serverError().entity(sb.toString()).build();
            } else {
                sb.append(errorMessage);
                return Response.serverError().entity(sb.toString()).build();
            }
        }
    }

    private String buildErrorMessage(HttpServletRequest req) {
        StringBuilder message = new StringBuilder();
        message.append("\nResource: ").append(getOriginalURL(req));
        message.append("\nMethod: ").append(req.getMethod());
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
