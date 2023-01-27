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

package com.github.fge.jsonpatch;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.github.fge.jackson.jsonpointer.JsonPointerCustom;
import com.github.fge.jackson.jsonpointer.JsonPointerException;
import com.sun.org.slf4j.internal.Logger;
import com.sun.org.slf4j.internal.LoggerFactory;

import java.util.Map;


/**
 * JSON Patch {@code replace} operation
 *
 * <p>For this operation, {@code path} points to the value to replace, and
 * {@code value} is the replacement value.</p>
 *
 * <p>It is an error condition if {@code path} does not point to an actual JSON
 * value.</p>
 */
public final class ReplaceOperation
        extends PathValueOperation {

  //  Logger logger = LoggerFactory.getLogger(ReplaceOperation.class);

    @JsonCreator
    public ReplaceOperation(@JsonProperty("path") final JsonPointerCustom path,
                            @JsonProperty("value") final JsonNode value,
                            @JsonProperty("originalValue") final JsonNode originalValue)
    {
        super("replace", path, value,originalValue);
    }


    @Override
    public JsonNode apply(final JsonNode node)
            throws JsonPatchException {

        if (path.path(node).isMissingNode())
            throw new JsonPatchException(BUNDLE.getMessage(
                    "jsonPatch.noSuchPath"));

        final JsonNode replacement = value.deepCopy();
        if (path.isEmpty())
            return replacement;
        final JsonNode ret = node.deepCopy();
        final JsonNode parent = path.parent().get(ret);
        final String rawToken = Iterables.getLast(path).getToken().getRaw();
        if (parent.isObject())
            ((ObjectNode) parent).replace(rawToken, replacement);
        else
            ((ArrayNode) parent).set(Integer.parseInt(rawToken), replacement);
        return ret;
    }

    private int getNodeToUpdate(JsonNode valueLocatorNode, ArrayNode array) {
        if (array == null) return -1;

        for (int i = 0; i < array.size(); i++) {
            JsonNode currentNode = array.get(i);
            ObjectMapper mapper = new ObjectMapper();
            //convert valueLocatorNode to map
            Map<String, Object> valueLocatorMap = mapper.convertValue(valueLocatorNode, new TypeReference<Map<String, Object>>() {
            });
            //make flag true
            boolean flag = true;
            //check if current node has value that are equal to value locator node
            for (Map.Entry<String, Object> entry : valueLocatorMap.entrySet()) {
                String currentKey = entry.getKey();
                //if keys are present
                if (!valueLocatorNode.get(currentKey).equals(currentNode.get(currentKey))) {
                    flag = false;
                    break;
                }
            }
            //if all values that are in the value locator node are present then return the index
            if (flag) return i;
        }
        //if node or path not found
        return -1;
    }

    private void applyStrictValidation(boolean flag) throws JsonPatchException {
        if (flag) {
            throw new JsonPatchException(BUNDLE.getMessage("jsonPatch.noSuchPath"));
        } else {
           // logger.error("jsonPatch.noSuchPath");
        }
    }

    private JsonNode getNodeToUpdate(boolean flag, JsonNode valueLocatorNode, ArrayNode arrayNode) throws JsonPatchException {
        //taking index of nodes that we want to update using valueLocatorNode
        int indToUpdate = getNodeToUpdate(valueLocatorNode, arrayNode);
        if (indToUpdate == -1) applyStrictValidation(flag);
        //get that node which we want to update
        return arrayNode.get(indToUpdate);
    }

    private ArrayNode getArrayNode(JsonNode node, boolean flag) throws JsonPatchException {
        //get pointer of array node in which we have the node that we will update using value field

        JsonPointerCustom arrayNodePath = null;
        try {
            arrayNodePath = JsonPointerCustom.getBeforeUnknown(path.toString());
        } catch (JsonPointerException e) {
            //logger.error(e.getMessage());
        }

        //getting the raw token of that array node
        String raw = null;
        if (arrayNodePath != null) {
            raw = Iterables.getLast(arrayNodePath).getToken().getRaw();
        } else
            applyStrictValidation(flag);
        //get the arrayNode
        return (ArrayNode) node.get(raw);
    }

    @Override
    public JsonNode apply(JsonNode node, boolean flag) throws JsonPatchException {
//
//        if (path == null || value_locator == null) {
//            applyStrictValidation(flag);
//            return node;
//        }
//        JsonNode result = null;
//
//        //if path is not null
//        if (path.toString().contains("?")) {
//
//            //get value locator copy
//            JsonNode valueLocatorNode = value_locator.deepCopy();
//
//            //get arrayNode in which we will find the node to update
//            ArrayNode arrayNode = getArrayNode(node, flag);
//
//            if (arrayNode == null) {
//                applyStrictValidation(flag);
//                return node;
//            }
//
//            //getting the actual node to update
//            JsonNode nodeToUpdate = getNodeToUpdate(flag, valueLocatorNode, arrayNode);
//
//            if (nodeToUpdate == null) return node;
//            //getting the field in which update is need to be made
//            String rawTokenField = getRawTokenFieldToUpdate();
//
//            //update the node >> updatedNode
//            if (nodeToUpdate.isObject() && rawTokenField != null) {
//                ((ObjectNode) nodeToUpdate).replace(rawTokenField, value);
//            } else if (rawTokenField != null) {
//                ((ArrayNode) nodeToUpdate).set(Integer.parseInt(rawTokenField), value);
//            }
//
//            result = node;
//
//        } else {
//            result = apply(node);
//        }
        return null;
    }

    private String getRawTokenFieldToUpdate() {
        //get the pointer of the node where we want to update value of the value field
        JsonPointerCustom toUpdateNodePointer = null;
        try {
            //taking pointer by using getAfterUnknown function which will return /Entitlements/?/Entitlement Key >> Entitlement Key
            toUpdateNodePointer = JsonPointerCustom.getAfterUnknown(path.toString());
        } catch (JsonPointerException e) {
           // logger.error("JsonPointerException: " + e.getMessage());
        }
        //get raw token to update the specified field
        String rawToken = null;
        if (toUpdateNodePointer != null) {
            rawToken = Iterables.getLast(toUpdateNodePointer).getToken().getRaw();
        }
        return rawToken;
    }


}

