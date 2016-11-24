/*******************************************************************************
 * Copyright 2016 Ivan Shubin https://github.com/ishubin/isolator4j
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.mindengine.isolator4j;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.HttpClientBuilder;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeMethod;
import java.io.IOException;
import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static java.lang.String.format;
import static org.mockito.Mockito.reset;

public abstract class ResourceTestBase {
    private Logger LOG = LoggerFactory.getLogger(getClass());

     final String mockUniqueKey = provideMockUniqueKey();

    /**
     * This method is supposed to give a name of a cookie which contains mock unique key for controlling all mocks
     * @return
     */
    public abstract String getMockCookieName();

    public <T> T mockIt(Class<T> mockClass) {
        return Mockito.mock(mockClass);
    }

    private String provideMockUniqueKey() {
        String id = UUID.randomUUID().toString();
        LOG.info(format("Generating mock unique key %s for test %s", id, getClass().getSimpleName()));
        return id;
    }
    private List<Object> mocks = new LinkedList<>();

    @BeforeMethod
    public void resetAllMocks() {
        reset(mocks.toArray(new Object[mocks.size()]));
    }

    protected <T> T registerMock(Class<T> mockClass) {
        return registerMock(mockIt(mockClass), mockClass);
    }

    protected <T> T registerMock(T mock, Class<T> mockClass) {
        MockRegistry.registerMock(mockUniqueKey, mock, mockClass.getName());
        mocks.add(mock);
        return mock;
    }

    private HttpClient client = HttpClientBuilder.create()
        .setConnectionTimeToLive(5, TimeUnit.SECONDS)
        .setMaxConnTotal(20)
        .build();


    public abstract String getTestUrl();

    public Response getJson(String path) throws IOException {
        HttpGet httpGet = new HttpGet(createUri(path));
        return executeRequest(httpGet);
    }

    private URI createUri(String path) {
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        return URI.create(getTestUrl() + path);
    }

    public Response postJson(String path, String requestBody) throws IOException {
        HttpPost httpPost = new HttpPost(createUri(path));
        StringEntity entity = new StringEntity(requestBody);
        httpPost.setEntity(entity);
        return executeRequest(httpPost);
    }

    private Response executeRequest(HttpRequestBase httpRequestBase) throws IOException {
        httpRequestBase.setHeader("Content-Type", "application/json");
        httpRequestBase.setHeader("Cookie", getMockCookieName() + "=" + mockUniqueKey);
        HttpResponse response = client.execute(httpRequestBase);
        int code = response.getStatusLine().getStatusCode();
        String textResponse = IOUtils.toString(response.getEntity().getContent());
        return new Response(code, textResponse);
    }
}
