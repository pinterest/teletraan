package com.pinterest.deployservice.bean;

public class BaseBean {

    /**
     * Trims the input string to the specified size limit. If the input string's length
     * exceeds the limit, the method returns the substring from the end of the string
     * with the specified limit. Otherwise returns the original string.
     *
     * @param value the input string to be trimmed
     * @param limit the maximum length of the returned string
     * @return the trimmed string if the input string's length exceeds the limit,
     *         otherwise the original string
     */
    protected String getStringWithSizeLimit(String value, int limit) {
        if (value != null && value.length() > limit) {
            return value.substring(value.length() - limit, value.length());
        }
        return value;
    }
}
