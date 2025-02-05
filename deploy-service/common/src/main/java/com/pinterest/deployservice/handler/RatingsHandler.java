/**
 * Copyright (c) 2016-2024 Pinterest, Inc.
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
package com.pinterest.deployservice.handler;

import com.pinterest.deployservice.ServiceContext;
import com.pinterest.deployservice.bean.RatingBean;
import com.pinterest.deployservice.common.CommonUtils;
import com.pinterest.deployservice.dao.RatingDAO;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Handler for user ratings and feedback */
public class RatingsHandler {
    private static final Logger LOG = LoggerFactory.getLogger(RatingsHandler.class);
    private static final long TIME_BETWEEN_USER_FEEDBACK =
            (86400000L * 90L); // # Milliseconds in 90 days
    private RatingDAO ratingDAO;

    public RatingsHandler(ServiceContext serviceContext) {
        ratingDAO = serviceContext.getRatingDAO();
    }

    public RatingDAO getRatingDAO() {
        return ratingDAO;
    }

    public String createRating(RatingBean bean) throws Exception {
        cleanOptOutRatings(bean.getAuthor());
        String id = CommonUtils.getBase64UUID();
        bean.setRating_id(id);
        ratingDAO.insert(bean);
        LOG.info("Successfully created rating: " + bean);
        return id;
    }

    private void cleanOptOutRatings(String author) throws Exception {
        List<RatingBean> ratingBeans = ratingDAO.getRatingsByAuthor(author);
        for (RatingBean bean : ratingBeans) {
            if (bean.getFeedback().isEmpty() && bean.getRating().equals("-1")) {
                ratingDAO.delete(bean.getRating_id());
            }
        }
    }

    public boolean checkUserFeebackStatus(String user) throws Exception {
        List<RatingBean> ratingBeans = ratingDAO.getRatingsByAuthor(user);
        if (ratingBeans.size() > 0) {
            // Get most recent feedback identifier
            RatingBean rating = ratingBeans.get(ratingBeans.size() - 1);
            long now = System.currentTimeMillis();
            long timestamp = rating.getTimestamp();
            // Been past a determined point between feedback
            if (now - timestamp > TIME_BETWEEN_USER_FEEDBACK) {
                return true;
            }
        } else {
            // No feedback data, eligible for feedback request
            return true;
        }

        // Too soon for new feedback request
        return false;
    }
}
