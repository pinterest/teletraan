/**
 * Copyright 2022 Pinterest, Inc.
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

package com.pinterest.deployservice.common;

import com.pinterest.deployservice.rodimus.RodimusManager;
import com.pinterest.deployservice.rodimus.RodimusManagerImpl;
import com.pinterest.deployservice.common.DeployInternalException;
import com.pinterest.deployservice.knox.Knox;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.*;

public class KnoxStripLnLfTest {

    private RodimusManager rodimusManager = null;
    private Knox mockKnox;
    private byte[] testKey = null;
    Method classRefreshCachedKey;
    Field classCachedKey;

    @Before
    public void setUp() throws Exception {

        // Create mock for Knox
        mockKnox = Mockito.mock(Knox.class);
        Mockito.when(this.mockKnox.getPrimaryKey()).thenAnswer(
            invocation -> this.getPrimaryKey(invocation) );

        // Create RodimusManagerImpl to test, if not already created
        if ( rodimusManager == null ) {
            rodimusManager = new RodimusManagerImpl( "http://localhost", "teletraan:test" );
        }

        // Mock fsKnox inside rodiumusManager
        this.mockClasses(this.rodimusManager, this.mockKnox );

        // Make refresh key accesible
        classRefreshCachedKey = rodimusManager.getClass().getDeclaredMethod("refreshCachedKey");
        classRefreshCachedKey.setAccessible(true);

        // Make cached key accesible
        classCachedKey = rodimusManager.getClass().getDeclaredField("cachedKey");
        classCachedKey.setAccessible(true);
    }

    @After
    public void tearDown() throws Exception {
    }



    // ### strip tests ###

    @Test
    public void noStripTest() throws Exception {
        String testString = "aa\naaa";
        this.testKey = testString.getBytes(); // set testing string
        this.classRefreshCachedKey.invoke(this.rodimusManager); // execute refreshCachedKey
        String cachedKey = (String)this.classCachedKey.get(this.rodimusManager); // get cached string
        Assert.assertEquals( cachedKey, testString ); // assert we have what is expected
    }

    @Test
    public void singleStripTest() throws Exception {
        String cmpString = "aa\naaa";
        String testString = "aa\naaa\n";
        this.testKey = testString.getBytes(); // set testing string
        this.classRefreshCachedKey.invoke(this.rodimusManager); // execute refreshCachedKey
        String cachedKey = (String)this.classCachedKey.get(this.rodimusManager); // get cached string
        Assert.assertEquals( cachedKey, cmpString ); // assert we have what is expected
    }

    @Test
    public void multipleStripTest() throws Exception {
        String cmpString = "aa\naaa";
        String testString = "aa\naaa\n\n\n";
        this.testKey = testString.getBytes(); // set testing string
        this.classRefreshCachedKey.invoke(this.rodimusManager); // execute refreshCachedKey
        String cachedKey = (String)this.classCachedKey.get(this.rodimusManager); // get cached string
        Assert.assertEquals( cachedKey, cmpString ); // assert we have what is expected
    }



    // ### HELPER METHODS ###

    private void mockClasses(RodimusManager rodimusMngr, Knox mokKnox) throws Exception {

        // Modify fsKnox to use our mock
        Field classKnox = rodimusMngr.getClass().getDeclaredField("fsKnox");
        classKnox.setAccessible(true);
        classKnox.set(rodimusMngr, mokKnox);
        classKnox.setAccessible(false);
    }

    private Object getPrimaryKey(InvocationOnMock invocation) throws Exception {
        return this.testKey;
    }


}