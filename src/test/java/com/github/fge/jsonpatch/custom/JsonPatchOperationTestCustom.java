/*
 * Copyright (c) 2014, Francis Galiegue (fgaliegue@gmail.com)
 *
 * This software is dual-licensed under:
 *
 * - the Lesser General Public License (LGPL) version 3.0 or, at your option, any
 *   later version;
 * - the Apache Software License (ASL) version 2.0.
 *
 * The text of this file and of both licenses is available at the root of this
 * project or, if you have the jar distribution, in directory META-INF/, under
 * the names LGPL-3.0.txt and ASL-2.0.txt respectively.
 *
 * Direct link to the sources:
 *
 * - LGPL 3.0: https://www.gnu.org/licenses/lgpl-3.0.txt
 * - ASL 2.0: http://www.apache.org/licenses/LICENSE-2.0.txt
 */

package com.github.fge.jsonpatch.custom;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectReader;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.JsonNumEquals;
import com.github.fge.jsonpatch.JsonPatchException;
import com.github.fge.jsonpatch.JsonPatchMessages;
import com.github.fge.jsonpatch.JsonPatchOperation;
import com.github.fge.msgsimple.bundle.MessageBundle;
import com.github.fge.msgsimple.load.MessageBundles;
import com.google.common.collect.Lists;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNotSame;
import static org.testng.Assert.assertTrue;
import static org.testng.Assert.fail;

@Test
public abstract class JsonPatchOperationTestCustom {
    private static final MessageBundle BUNDLE
            = MessageBundles.getBundle(JsonPatchMessages.class);

    private static final JsonNumEquals EQUIVALENCE
            = JsonNumEquals.getInstance();

    private final JsonNode errors;
    private final JsonNode ops;
    private final ObjectReader reader;

    protected JsonPatchOperationTestCustom(final String prefix)
            throws IOException {
        final String resource = "/jsonpatch/" + prefix + ".json";
        final JsonNode node = JsonLoader.fromResource(resource);
        errors = node.get("errors");
        ops = node.get("ops");
        reader = JacksonUtils.getReader().forType(JsonPatchOperation.class);
    }

    @DataProvider
    public final Iterator<Object[]> getErrors()
            throws NoSuchFieldException, IllegalAccessException {
        final List<Object[]> list = Lists.newArrayList();
        for (final JsonNode node : errors)
            list.add(new Object[]{
                    node.get("op"),
                    node.get("node"),
                    BUNDLE.getMessage(node.get("message").textValue())
            });
        return list.iterator();
    }

    @Test(dataProvider = "getErrors")
    public final void errorsAreCorrectlyReported(final JsonNode patch,
                                                 final JsonNode node, final String message)
            throws IOException {
        final JsonPatchOperation op = reader.readValue(patch);

        try {
            op.apply(node, true);
            fail("No exception thrown!!");
        } catch (JsonPatchException e) {
            assertEquals(e.getMessage(), message);
        }
    }

    @Test(dataProvider = "getErrors")
    public final void errorsAreCorrectlyReportedForFalse(final JsonNode patch,
                                                         final JsonNode node, final String message)
            throws IOException {
        final JsonPatchOperation op = reader.readValue(patch);

        try {
            op.apply(node, false);
//            fail("No exception thrown!!");
        } catch (JsonPatchException e) {
            assertEquals(e.getMessage(), message);
        }
    }

    @DataProvider
    public final Iterator<Object[]> getOps() {
        final List<Object[]> list = Lists.newArrayList();

        for (final JsonNode node : ops)
            list.add(new Object[]{
                    node.get("op"),
                    node.get("node"),
                    node.get("expected")
            });

        return list.iterator();
    }


    @Test(dataProvider = "getOps")
    public final void operationsYieldExpectedResults(final JsonNode patch,
                                                     final JsonNode node, final JsonNode expected)
            throws IOException, JsonPatchException {

        final JsonPatchOperation op = reader.readValue(patch);
        final JsonNode actual = op.apply(node, true);

        assertTrue(EQUIVALENCE.equivalent(actual, expected),
                "patched node differs from expectations: expected " + expected
                        + " but found " + actual);

        if (EQUIVALENCE.equivalent(node, actual) && node.isContainerNode())
            assertNotSame(node.toPrettyString(), actual.toPrettyString(),
                    "operation didn't make a copy of the input node");
    }

    @Test(dataProvider = "getOps")
    public final void operationsYieldExpectedResultsForFalse(final JsonNode patch,
                                                             final JsonNode node, final JsonNode expected)
            throws IOException, JsonPatchException {

        final JsonPatchOperation op = reader.readValue(patch);
        final JsonNode actual = op.apply(node, false);

        assertTrue(EQUIVALENCE.equivalent(actual, expected),
                "patched node differs from expectations: expected " + expected
                        + " but found " + actual);

        if (EQUIVALENCE.equivalent(node, actual) && node.isContainerNode())
            assertNotSame(node.toPrettyString(), actual.toPrettyString(),
                    "operation didn't make a copy of the input node");
    }

}



