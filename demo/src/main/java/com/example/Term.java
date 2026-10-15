package com.example;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Academic term information returned by Eastern's registration system.
 *
 * @param code the term code used internally to query course sections
 * @param description the readable term description shown in the user interface
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record Term(String code, String description) {

    /**
     * Returns the user-facing term label.
     *
     * @return the term description
     */
    @Override
    public String toString() {
        return description;
    }
}
