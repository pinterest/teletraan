/**
 * Copyright 2016 Pinterest, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.pinterest.deployservice.bean;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;

/**
 * create table user_ratings (
 * rating_id  VARCHAR(32)     NOT NULL,
 * author     VARCHAR(32)     NOT NULL,
 * rating     VARCHAR(8)      NOT NULL,
 * feedback   VARCHAR(4096)   NOT NULL,
 * timestamp  BIGINT          NOT NULL,
 * PRIMARY KEY (rating_id)
 * );
 */
public class RatingBean implements Updatable {
    @JsonProperty("id")
    private String rating_id;

    private String author;

    private String rating;

    private String feedback;

    @JsonProperty("createDate")
    private Long timestamp;

    public String getRating_id() {
        return rating_id;
    }

    public void setRating_id(String rating_id) {
        this.rating_id = rating_id;
    }

    public String getAuthor() {
        return author;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public String getRating() {
        return rating;
    }

    public void setRating(String rating) {
        this.rating = rating;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public SetClause genSetClause() {
        SetClause clause = new SetClause();
        clause.addColumn("rating_id", rating_id);
        clause.addColumn("author", author);
        clause.addColumn("rating", rating);
        clause.addColumn("feedback", feedback);
        clause.addColumn("timestamp", timestamp);
        return clause;
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this);
    }

}
