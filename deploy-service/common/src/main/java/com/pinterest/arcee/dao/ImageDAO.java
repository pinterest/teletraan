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
package com.pinterest.arcee.dao;


import com.pinterest.arcee.bean.ImageBean;

import java.util.List;

public interface ImageDAO {
    public void insertOrUpdate(ImageBean amiBean) throws Exception;

    public ImageBean getById(String amiId) throws Exception;

    public List<ImageBean> getImages(String appName, int pageIndex, int pageSize) throws Exception;

    public List<String> getAppNames() throws Exception;

    public void delete(String amiId) throws Exception;
}
