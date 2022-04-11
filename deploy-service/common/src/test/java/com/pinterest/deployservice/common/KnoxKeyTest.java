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

import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.Assert;

import org.mockito.Mockito;
import org.mockito.invocation.InvocationOnMock;

import java.util.*;

public class KnoxKeyTest {

    private static enum Answer { NULL, EXCEPTION, ARRAY, LATENCY };

    private static String msgUnauthException =
        "HTTP request failed, status = 401, content = Unauthorized";
    private static String postAnswerTag = 
        "{\"i-001\":{\"Name\": \"devapp-example1\"},\"i-002\":{\"Name\": \"devrestricted-example2\"}}";
    private static String postAnswerArray =
        "[\"i-001\",\"i-002\"]";
    private static String getAnswerValue = 
        "{\"launchLatencyTh\": 10}";

    private RodimusManager rodimusManager = null;    
    private Knox mockKnox;
    private HTTPClient mockHttpClient;
    private List<Answer> answerList;
    private byte[][] testKey = new byte[2][];
    private int rodimusManagerRETRIES;
    private String postAnswerReturn = null;
    private boolean swapKey = false;

    @Before
    public void setUp() throws Exception {
        // Load testKeys
        testKey[0] = "aaa".getBytes(); // auth error
        testKey[1] = "bbb".getBytes(); // auth ok

        // Create mock for Knox
        mockKnox = Mockito.mock(Knox.class);

        // Create mock for httpClient
        mockHttpClient = Mockito.mock(HTTPClient.class);

        // Create RodimusManagerImpl to test, if not already created
        if ( rodimusManager == null ) {
                rodimusManager = new RodimusManagerImpl( "http://localhost", "teletraan:test" );
        }

        // Allocate answerList
        answerList = new ArrayList<Answer>();
    }
  
    @After
    public void tearDown() throws Exception {
    }





    // ### terminateHostsByClusterName tests ###

    @Test
    public void thbcnOk() throws Exception {
        // terminateHostsByClusterName
        // All working as expected

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        Mockito.when(this.mockHttpClient.delete(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.deleteAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        try{
            this.rodimusManager.terminateHostsByClusterName("cluster",Collections.singletonList("i-001"));
        }catch( Exception e ){
            Assert.assertTrue( "Unexpected exception: " + e, false );
        }

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.NULL); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }

    @Test
    public void thbcnNoAuth() throws Exception {
        // terminateHostsByClusterName
        // Token does not work, refresh does not offer new token

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0]);

        Mockito.when(this.mockHttpClient.delete(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.deleteAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        Exception exception = Assert.assertThrows( DeployInternalException.class, () -> {
            this.rodimusManager.terminateHostsByClusterName("cluster",Collections.singletonList("i-001"));
            } 
        );

        Assert.assertTrue( exception.getMessage().contains(msgUnauthException) );

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.EXCEPTION); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }





    // ### getTerminatedHosts tests ###

    @Test
    public void gthOk() throws Exception {
        // getTerminatedHosts
        // All working as expected

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        Mockito.when(this.mockHttpClient.post(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.postAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        this.postAnswerReturn = this.postAnswerArray;

        Collection<String> res = null;
        try{
            res = this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001","i-002"));
        }catch( Exception e ){
            Assert.assertTrue( "Unexpected exception: " + e, false );
        }

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.ARRAY); }};

        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }

    @Test
    public void gthNoAuth() throws Exception {
        // getTerminatedHosts
        // Token does not work, refresh does not offer new token

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0]);

        Mockito.when(this.mockHttpClient.post(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.postAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        this.postAnswerReturn = this.postAnswerArray;

        Exception exception = Assert.assertThrows( DeployInternalException.class, () -> {
            this.rodimusManager.getTerminatedHosts(Arrays.asList("i-001","i-002"));
            } 
        );

        Assert.assertTrue( exception.getMessage().contains(msgUnauthException) );

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.EXCEPTION); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }





    // ### getClusterInstanceLaunchGracePeriod tests

    @Test
    public void gcilgpOk() throws Exception {
        // getClusterInstanceLaunchGracePeriod
        // All working as expected

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        Mockito.when(this.mockHttpClient.get(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.getAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        long res = 0;
        try{
            res = this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
        }catch( Exception e ){
            Assert.assertTrue( "Unexpected exception: " + e, false );
        }

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.LATENCY); }};

        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
        Assert.assertEquals( res, (long)10 );
    }

    @Test
    public void gcilgpNoAuth() throws Exception {
        // getClusterInstanceLaunchGracePeriod
        // Token does not work, refresh does not offer new token

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0]);

        Mockito.when(this.mockHttpClient.get(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.getAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        Exception exception = Assert.assertThrows( DeployInternalException.class, () -> {
            this.rodimusManager.getClusterInstanceLaunchGracePeriod("cluster");
            }
        );

        Assert.assertTrue( exception.getMessage().contains("HTTP request failed, status") );

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.EXCEPTION); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }





    // ### getEC2Tags tests ###

    @Test
    public void ge2tOk() throws Exception {
        // getEC2Tags
        // All working as expected

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[1]);

        Mockito.when(this.mockHttpClient.post(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.postAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        this.postAnswerReturn = this.postAnswerTag;

        Map<String, Map<String, String>> res = null;
        try{
            res = this.rodimusManager.getEc2Tags(Arrays.asList("i-001","i-002"));
        }catch( Exception e ){
            Assert.assertTrue( "Unexpected exception: " + e, false );
        }

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.ARRAY); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }

    @Test
    public void ge2tNoAuth() throws Exception {
        // getEC2Tags
        // Token does not work, refresh does not offer new token

        Mockito.when(this.mockKnox.getPrimaryKey()).thenReturn(this.testKey[0]);

        Mockito.when(this.mockHttpClient.post(
            Mockito.any(String.class),
            Mockito.any(String.class),
            Mockito.any(Map.class),
            Mockito.any(Integer.class))).thenAnswer( invocation -> this.postAnswer(invocation) );

        this.mockClasses(this.rodimusManager, this.mockKnox, this.mockHttpClient );

        this.postAnswerReturn = this.postAnswerTag;

        Exception exception = Assert.assertThrows( DeployInternalException.class, () -> {
            this.rodimusManager.getEc2Tags(Arrays.asList("i-001","i-002"));
            } 
        );

        Assert.assertTrue( exception.getMessage().contains("HTTP request failed, status") );

        final ArrayList<Answer> cmpArray = new ArrayList<Answer>() {{ add(Answer.EXCEPTION); }};
        Assert.assertArrayEquals( this.answerList.toArray(), cmpArray.toArray() );
    }





// =======================================================

// to test: methods: terminateHostsByClusterName         1
//                   getTerminatedHosts                  2
//                   getClusterInstanceLaunchGracePeriod 3
//                   getEc2Tags                          4
//          actions: token ok                                        1234
//                     (everyday operation)
//                   token no authorization                          1234

// =======================================================



// ### HELPER METHODS ###

    private void mockClasses(RodimusManager rodimusMngr, Knox mokKnox, HTTPClient mokHttpClient) throws Exception {

        // Modify fsKnox to use our mock
        Field classKnox = rodimusMngr.getClass().getDeclaredField("fsKnox");
        classKnox.setAccessible(true);
        classKnox.set(rodimusMngr, mokKnox);
        classKnox.setAccessible(false);

        // Modify httpClient to use our mock
        Field classHttpClient = rodimusMngr.getClass().getDeclaredField("httpClient");
        classHttpClient.setAccessible(true);
        classHttpClient.set(rodimusMngr, mokHttpClient);
        classHttpClient.setAccessible(false);
    }

    private String getToken(Map<String, String> headers) {
        // Get token out of Map of headers

        for( Map.Entry<String, String> entry : headers.entrySet() )
        {
// DEBUG             System.out.println("headers-> " + entry.getKey() + ":" + entry.getValue());
            if( entry.getKey()=="Authorization" ) return entry.getValue();
        }
        return null;
    }

    private Object deleteAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "DELETE" answer method
        Object[] args = invocation.getArguments();
        Object mock = invocation.getMock();

        Map<String, String> headers = (Map<String, String>) args[2];
        String token = getToken( headers );

        if ( Objects.equals( token, "token bbb" ) )
        {
            this.answerList.add(Answer.NULL);
            return null;
        }else{
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

    private Object postAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "POST" answer method
        Object[] args = invocation.getArguments();
        Object mock = invocation.getMock();

        Map<String, String> headers = (Map<String, String>) args[2];
        String token = getToken( headers );

        if ( Objects.equals( token, "token bbb" ) )
        {
            this.answerList.add(Answer.ARRAY);
            return this.postAnswerReturn;
        }else{
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

    private Object getAnswer(InvocationOnMock invocation) throws Exception {
        // HTTPClient "GET" answer method
        Object[] args = invocation.getArguments();
        Object mock = invocation.getMock();

        Map<String, String> headers = (Map<String, String>) args[3];
        String token = getToken( headers );

        if ( Objects.equals( token, "token bbb" ) )
        {
            this.answerList.add(Answer.LATENCY);
            return this.getAnswerValue;
        }else{
            this.answerList.add(Answer.EXCEPTION);
            throw new DeployInternalException(this.msgUnauthException);
        }
    }

}