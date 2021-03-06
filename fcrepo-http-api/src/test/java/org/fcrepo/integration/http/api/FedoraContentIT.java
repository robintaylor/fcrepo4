/**
 * Copyright 2014 DuraSpace, Inc.
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
package org.fcrepo.integration.http.api;

import static java.util.TimeZone.getTimeZone;
import static javax.ws.rs.core.Response.Status.CREATED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.fcrepo.http.api.ContentExposingResource;
import org.junit.Test;

import com.hp.hpl.jena.graph.Node;
import com.hp.hpl.jena.graph.NodeFactory;
import com.hp.hpl.jena.update.GraphStore;
import com.sun.jersey.core.header.ContentDisposition;

/**
 * <p>FedoraContentIT class.</p>
 *
 * @author awoods
 */
public class FedoraContentIT extends AbstractResourceIT {

    private static final String faulkner1 =
            "The past is never dead. It's not even past.";

    @Test
    public void testAddDatastream() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final HttpResponse response = createDatastream(pid, "zxc", "foo");

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + pid + "/zxc/fcr:content", location);

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));

        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testAddDatastreamByContentLocation() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost httpPost = postDSMethod(pid, "zxc", "");
        httpPost.addHeader("Content-Location", serverAddress);
        final HttpResponse response =
            client.execute(httpPost);
        assertEquals(CREATED.getStatusCode(), response.getStatusLine().getStatusCode());

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(
                        "Got wrong URI in Location header for datastream creation!",
                        serverAddress + pid + "/zxc/fcr:content", location);

        final HttpGet httpGet = new HttpGet(location);

        final HttpResponse contentResponse = client.execute(httpGet);

        final String body = IOUtils.toString(contentResponse.getEntity().getContent());

        assertTrue(body.contains(pid));


    }
    @Test
    public void testAddDeepDatastream() throws Exception {
        final String uuid = getRandomUniquePid();
        final HttpPost method = postDSMethod(uuid + "/does/not/exist/yet", "zxc", "foo");
        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals("Got wrong URI in Location header for datastream creation!", serverAddress + uuid +
                "/does/not/exist/yet/zxc/fcr:content", location);
    }

    @Test
    public void testPutDatastream() throws Exception {

        final String pid = getRandomUniquePid();
        createObject(pid);
        final HttpPut method =
                putDSMethod(pid, "zxc", "foo");
        final HttpResponse response = client.execute(method);
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());
        assertEquals(
                "Got wrong URI in Location header for datastream creation!",
                serverAddress + pid + "/zxc/fcr:content", location);

        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        final String lastmod = response.getFirstHeader("Last-Modified").getValue();
        assertNotNull("Should set Last-Modified for new nodes", lastmod);
        assertNotEquals("Last-Modified should not be blank for new nodes", lastmod.trim(), "");
    }

    @Test
    public void testPutDatastreamContentOnObject() throws Exception {
        final String content = "foo";
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPut put = new HttpPut(serverAddress + pid + "/fcr:content");
        put.setEntity(new StringEntity(content));
        final HttpResponse response = client.execute(put);
        assertEquals("Expected 400 response code when PUTing content on an object (as opposed to a datastream).",
                400, response.getStatusLine().getStatusCode());
    }

    @Test
    public void testPutDatastreamDuringTransaction() throws Exception {

        final String txLocation = createTransaction();
        final String pid = getRandomUniquePid();

        final String contentLocation = txLocation + "/" + pid + "/fcr:content";

        final HttpPut put = new HttpPut(contentLocation);
        put.setEntity(new StringEntity("foo content"));
        final HttpResponse response = execute(put);

        assertFalse("Found Last-Modified header within transaction!",
                response.containsHeader("Last-Modified"));
        assertFalse("Found ETag header within transaction!",
                response.containsHeader("ETag"));

        final HttpGet testGet = new HttpGet(contentLocation);
        final HttpResponse responseFromGet = execute(testGet);

        assertFalse("Found Last-Modified header within transaction!",
                responseFromGet.containsHeader("Last-Modified"));
        assertFalse("Found ETag header within transaction!",
                responseFromGet.containsHeader("ETag"));

    }

    @Test
    public void testPutDatastreamWithContentDisposition() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);
        final HttpPut method = putDSMethod(pid, "zxc", "foo");

        method.addHeader("Content-Disposition", "inline; filename=\"some-name\"");

        final HttpResponse response = client.execute(method);
        final String location = response.getFirstHeader("Location").getValue();
        assertEquals(201, response.getStatusLine().getStatusCode());

        final HttpGet getObjMethod = new HttpGet(serverAddress + pid + "/zxc");
        final GraphStore results = getGraphStore(getObjMethod);
        assertTrue("Didn't find original name!",
                   results.contains(Node.ANY,
                                    NodeFactory.createURI(location),
                                    NodeFactory.createURI("http://www.loc.gov/premis/rdf/v1#hasOriginalName"),
                                    NodeFactory.createLiteral("some-name")));

        final HttpGet getContentMethod = new HttpGet(location);
        final HttpResponse contentResponse = client.execute(getContentMethod);

        final ContentDisposition contentDisposition =
                new ContentDisposition(contentResponse.getFirstHeader("Content-Disposition").getValue());

        assertEquals("some-name", contentDisposition.getFileName());
    }

    @Test
    public void testMutateDatastream() throws Exception {
        final String pid = getRandomUniquePid();

        createObject(pid);

        final HttpPost createDataStreamMethod = postDSMethod(pid, "ds1", "foo");
        assertEquals("Couldn't create a datastream!", 201,
                getStatus(createDataStreamMethod));

        final HttpPut mutateDataStreamMethod = putDSMethod(pid, "ds1", "bar");
        mutateDataStreamMethod.setEntity(new StringEntity(faulkner1, "UTF-8"));

        final HttpResponse response = client.execute(mutateDataStreamMethod);
        assertTrue("Didn't find Last-Modified header!", response.containsHeader("Last-Modified"));
        assertTrue("Didn't find ETag header!", response.containsHeader("ETag"));

        final int status = response.getStatusLine().getStatusCode();

        if (status != 204) {
            logger.error(EntityUtils.toString(response.getEntity()));
        }

        assertEquals("Couldn't mutate a datastream!", 204, status);

        final HttpGet retrieveMutatedDataStreamMethod = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        assertTrue("Datastream didn't accept mutation!", faulkner1
                .equals(EntityUtils.toString(client.execute(
                        retrieveMutatedDataStreamMethod).getEntity())));
    }

    @Test
    public void testGetDatastreamContent() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);
        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        assertEquals(200, getStatus(method_test_get));

        final HttpResponse response = client.execute(method_test_get);

        logger.debug("Returned from HTTP GET, now checking content...");
        assertTrue("Got the wrong content back!", "marbles for everyone"
                .equals(EntityUtils.toString(response.getEntity())));
        assertEquals("urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df",
                response.getFirstHeader("ETag").getValue().replace("\"", ""));

        final ContentDisposition contentDisposition =
                new ContentDisposition(response.getFirstHeader("Content-Disposition").getValue());

        assertEquals("attachment", contentDisposition.getType());
        assertEquals("ds1", contentDisposition.getFileName());

        logger.debug("Content was correct.");
    }

    @Test
    public void testRefetchingDatastreamContent() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);


        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final SimpleDateFormat format =
                new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
        format.setTimeZone(getTimeZone("GMT"));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("If-None-Match",
                "\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_get.setHeader("If-Modified-Since", format
                .format(new Date()));

        assertEquals(304, getStatus(method_test_get));

    }

    @Test
    public void testConditionalPutOfDatastreamContent() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpPut method_test_put = new HttpPut(serverAddress + pid + "/ds1/fcr:content");
        method_test_put.setHeader("If-Match",
                "\"urn:sha1:ba6cb22191300aebcfcfb83de9635d6b224677df\"");
        method_test_put.setHeader("If-Unmodified-Since",
                "Sat, 29 Oct 1994 19:43:31 GMT");
        method_test_put.setEntity(new StringEntity("asdf"));

        assertEquals(412, getStatus(method_test_put));

    }

    @Test
    public void testRangeRequest() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=1-3");

        final HttpResponse response = client.execute(method_test_get);
        assertEquals(206, response.getStatusLine().getStatusCode());
        logger.debug("Returned from HTTP GET, now checking content...");
        assertEquals("Got the wrong content back!", "arb",EntityUtils.toString(response.getEntity()));
        assertEquals("bytes 1-3/20", response.getFirstHeader("Content-Range").getValue());

    }

    @Test
    public void testRangeRequestWithSkipBytes() throws Exception {
        ContentExposingResource.setMaxBufferSize(10);
        final String pid = getRandomUniquePid();
        createObject(pid);
        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "large marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=1-21");

        final HttpResponse response = client.execute(method_test_get);
        assertEquals(206, response.getStatusLine().getStatusCode());
        logger.debug("Returned from HTTP GET, now checking content...");
        assertEquals("Got the wrong content back!", "arge marbles for ever",
                EntityUtils.toString(response.getEntity()));
        assertEquals("bytes 1-21/26", response.getFirstHeader("Content-Range").getValue());

    }

    @Test
    public void testRangeRequestBadRange() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=50-100");

        final HttpResponse response = client.execute(method_test_get);
        assertEquals(416, response.getStatusLine().getStatusCode());
        assertEquals("bytes 50-100/20", response.getFirstHeader("Content-Range").getValue());

    }

    @Test
    public void testRangeRequestOpenEnded() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=2-");

        final HttpResponse response = client.execute(method_test_get);
        assertEquals(206, response.getStatusLine().getStatusCode());
        logger.debug("Returned from HTTP GET, now checking content...");
        assertEquals("Got the wrong content back!", "rbles for everyone",
                EntityUtils.toString(response.getEntity()));
        assertEquals("bytes 2-19/20", response.getFirstHeader("Content-Range").getValue());
    }

    @Test
    public void testRangeRequestOpenStart() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost createDSMethod = postDSMethod(pid, "ds1", "marbles for everyone");
        assertEquals(201, getStatus(createDSMethod));

        final HttpGet method_test_get = new HttpGet(serverAddress + pid + "/ds1/fcr:content");
        method_test_get.setHeader("Range", "bytes=-2");

        final HttpResponse response = client.execute(method_test_get);
        assertEquals(206, response.getStatusLine().getStatusCode());
        logger.debug("Returned from HTTP GET, now checking content...");
        assertEquals("Got the wrong content back!", "mar",
                EntityUtils.toString(response.getEntity()));
        assertEquals("bytes 0-2/20", response.getFirstHeader("Content-Range").getValue());
    }

    @Test
    public void testPostToExistingDS() throws Exception {
        final String pid = getRandomUniquePid();
        createObject(pid);

        final HttpPost postDSMethod = postDSMethod(pid, "ds1", "foo");
        assertEquals("Posting should work!", 201, getStatus(postDSMethod));

        final HttpPost repostDSMethod = postDSMethod(pid, "ds1", "bar");
        assertEquals("Reposting should error!", 409, getStatus(repostDSMethod));
    }

    @Test
    public void testPostToExistingDSIndirect() throws Exception {
        final String pid = getRandomUniquePid();
        final HttpPost postDSMethod = new HttpPost(serverAddress);
        postDSMethod.addHeader("Slug", pid);
        postDSMethod.setEntity(new StringEntity("foo", "UTF-8"));
        postDSMethod.addHeader("Content-Type", "application/foo");
        assertEquals(201, getStatus(postDSMethod));

        final HttpPost repostDSMethod = new HttpPost(serverAddress + pid);
        repostDSMethod.setEntity(new StringEntity("bar", "UTF-8"));
        repostDSMethod.addHeader("Content-Type", "application/foo");
        assertEquals(409, getStatus(repostDSMethod));
    }

}
