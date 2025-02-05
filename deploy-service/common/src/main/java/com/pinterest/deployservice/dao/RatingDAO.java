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
package com.pinterest.deployservice.dao;

import com.pinterest.deployservice.bean.RatingBean;
import java.util.List;

/** A collection of methods to help interact with the user_ratings table. */
public interface RatingDAO {

    void insert(RatingBean bean) throws Exception;

    void delete(String id) throws Exception;

    List<RatingBean> getRatingsInfos(int pageId, int pageSize) throws Exception;

    List<RatingBean> getRatingsByAuthor(String author) throws Exception;
}
