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

package com.github.fge.jsonpatch.diff;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jackson.JacksonUtils;
import com.github.fge.jackson.JsonLoader;
import com.github.fge.jackson.jsonpointer.JsonPointer;
import com.github.fge.jackson.jsonpointer.JsonPointerCustom;
import com.github.fge.jsonpatch.JsonPatch;
import com.google.common.collect.Lists;
import org.testng.Assert;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.testng.Assert.assertEquals;

public final class UnchangedTest {
    private static final ObjectMapper MAPPER = JacksonUtils.newMapper();
    private static final TypeReference<Map<JsonPointerCustom, JsonNode>> TYPE_REF = new TypeReference<Map<JsonPointerCustom, JsonNode>>() {
    };

    private final JsonNode testData;

    public UnchangedTest()
            throws IOException {
        final String resource = "/jsonpatch/diff/unchanged.json";
        testData = JsonLoader.fromResource(resource);
    }

    @DataProvider
    public Iterator<Object[]> getTestData()
            throws IOException {
        final List<Object[]> list = Lists.newArrayList();

        for (final JsonNode node : testData)
            list.add(new Object[]{node.get("first"), node.get("second"),
                    MAPPER.readValue(node.get("unchanged").traverse(), TYPE_REF)});

        return list.iterator();
    }

    @Test(dataProvider = "getTestData")
    public void computeUnchangedValuesWorks(final JsonNode first,
                                            final JsonNode second, final Map<JsonPointerCustom, JsonNode> expected) {
        final Map<JsonPointerCustom, JsonNode> actual
                = JsonDiff.getUnchangedValues(first, second);

        assertEquals(actual, expected);
    }


    @Test
    public void testCase1() throws IOException {

        final String resource_one = "/jsonpatch/diff/old_diff_custom.json";
        final JsonNode first = JsonLoader.fromResource(resource_one);

        final String resource_two = "/jsonpatch/diff/new_diff_custom.json";
        final JsonNode second = JsonLoader.fromResource(resource_two);

        JsonPointerCustom pointer = JsonPointerCustom.of("Entitlements");
        Map<JsonPointerCustom, Set<String>> map = new HashMap<>();
        Set<String> set = new HashSet<>();
        set.add("Application Key");
        set.add("Entitlement Type");
        set.add("Entitlement Name");
        map.put(pointer, set);

        JsonNode node = JsonDiff.asJson(first, second,map);
      //  Assert.assertEquals(node.findValue("value").toString(),"\"updated\"");
        System.out.println(node.toPrettyString());



    }

    @Test
    public void testCase2() throws IOException {
        String first = "{\n" +
                "  \"name\": \"hello\"\n" +
                "}";
        String second ="{\n" +
                "  \"name\": \"hello\"\n" +
                "}";
        String actual = "";
        JsonNode jsonNode1 = new ObjectMapper().readTree(first);
        JsonNode jsonNode2 = new ObjectMapper().readTree(second);
        JsonNode jsonNode3 = new ObjectMapper().readTree(actual);
        JsonNode jsonNode = JsonDiff.asJson(jsonNode1, jsonNode2,null);
        Assert.assertEquals(jsonNode,jsonNode3);
    }

    @Test
    public void testCase3() throws IOException {

        /** these code is converting string into jsonNode */
        String first = "{\n" +
                "  \"name\": \"hello\"\n" +
                "}";
        String second ="{\n" +
                "  \"name\": \"hello2\"\n" +
                "}";
        JsonNode jsonNode1 = new ObjectMapper().readTree(first);
        JsonNode jsonNode2 = new ObjectMapper().readTree(second);
        JsonNode jsonNode3 = JsonDiff.asJson(jsonNode1, jsonNode2,null);
        Assert.assertEquals(jsonNode3.findValue("op").toString(),"\"replace\"");
    }

    @Test
    public void testCase4() throws IOException {

        String first = "{\n" +
                "  \"name\": \"hello\"\n" +
                "}";
        String second ="{\n" +
                "  \"name\": \"hello\",\n" +
                "  \"surname\": \"hi\"\n" +
                "}";
        JsonNode jsonNode1 = new ObjectMapper().readTree(first);
        JsonNode jsonNode2 = new ObjectMapper().readTree(second);
        JsonNode jsonNode3 = JsonDiff.asJson(jsonNode1, jsonNode2,null);
        Assert.assertEquals(jsonNode3.findValue("op").toString(),"\"remove\"");
    }

    @Test
    public void testCase5() throws IOException {

        String first = "{\n" +
                "  \"name\": \"hello\",\n" +
                "  \"size\": \"12\"\n" +
                "}";
        String second ="{\n" +
                "  \"name\": \"hello\"\n" +
                "}";
        JsonNode jsonNode1 = new ObjectMapper().readTree(first);
        JsonNode jsonNode2 = new ObjectMapper().readTree(second);
        JsonNode jsonNode3 = JsonDiff.asJson(jsonNode1, jsonNode2,null);
        Assert.assertEquals(jsonNode3.findValue("op").toString(),"\"add\"");
    }

    @Test
    public void testCase6() throws IOException {

        String first = "{\n" +
                "  \"abc\": \"abc\",\n" +
                "  \"fruits\": [\n" +
                "    {\n" +
                "      \"name\": \"Apple\",\n" +
                "      \"image\": \"i1\",\n" +
                "      \"price\": 35\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        String second ="{\n" +
                "  \"abc\": \"abc\",\n" +
                "  \"fruits\": [\n" +
                "    {\n" +
                "      \"name\": \"Apple\",\n" +
                "      \"image\": \"i2\",\n" +
                "      \"price\": 35\n" +
                "    }\n" +
                "  ]\n" +
                "}";
        JsonNode jsonNode1 = new ObjectMapper().readTree(first);
        JsonNode jsonNode2 = new ObjectMapper().readTree(second);
        JsonNode jsonNode3 = JsonDiff.asJson(jsonNode1, jsonNode2,null);
        Assert.assertEquals(jsonNode3.findValue("op").toString(),"\"replace\"");
    }
}
